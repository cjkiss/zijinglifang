package com.zhutieju.testservice;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import com.tutk.sample.AVAPI.Client;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.util.Log;

/**
 * 服务端主Activity
 * 
 * @author Administrator
 * 
 */
public class MainActivity extends Activity {
	public static final String TAG = "服务端日志";
	// ServerThread thread;
	ReadFileThread thread;
	boolean isActivity = true;// 控制线程ServerThread的run方法
	long decoder;
	H264Android h264;
	byte[] mPixel = new byte[352 * 288 * 4];
	ByteBuffer buffer = ByteBuffer.wrap(mPixel);
	public static ArrayList<byte[]> framebuf = new ArrayList<byte[]>();
	static final String UID = "FR499HNEDW6FUNPPSZF";
	
	public static MainActivity mainActivity ;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mainActivity=this;
		setContentView(new MyView(this));
		h264 = new H264Android();
		decoder = h264.initDecoder(352, 288);
		int i = mPixel.length;
		for (i = 0; i < mPixel.length; i++) {
			mPixel[i] = (byte) 0x00;
		}
		// thread = new ServerThread();
		// 读取文件类
		// thread = new ReadFileThread();
		//thread.start();
		// 启动一个线程获得数据 保存起来
		
		new Thread() {
			public void run() {
				Client.start(MainActivity.this.UID);
			}
		}.start();		
		new Thread(new DecordeThread()).start();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (thread.isAlive()) {
			isActivity = false;
		}
		finish();
		System.exit(0);
	}

	/**
	 * 获取客户端数据的线程类
	 * 
	 * @author Administrator
	 * 
	 */
	public class ServerThread extends Thread {
		ServerSocket ss;// 服务端ServerSocket

		public ServerThread() {

		}

		Socket s;
		InputStream is;
		int len, size;
		StringBuilder sb;;

		@Override
		public void run() {
			try {
				Log.v(TAG, "服务端启动成功--------------->");
				ss = new ServerSocket(5000);
				s = ss.accept();
				Log.v(TAG, "与客户端连接成功------------->" + s.toString());
				DataInputStream dis = new DataInputStream(s.getInputStream());
				ByteArrayOutputStream baos;
				while (isActivity) {
					int len2 = 0;
					baos = new ByteArrayOutputStream();

					byte[] b = new byte[20];
					dis.read(b);
					try {
						// System.out.println("--->" + JTools.toStringList(b, 0,
						// "UTF-8").get(0));
					} catch (Exception e) {
						e.printStackTrace();
					}

					sb = new StringBuilder();
					while ((len = dis.read()) != '\r') {
						if (len == -1) {
							break;
						}
						sb.append((char) len);
					}
					if (sb.length() > 0) {
						size = Integer.parseInt(sb.toString());
						byte[] data = new byte[size];
						while (size > 0
								&& (len = dis.read(data, 0, size)) != -1) {
							baos.write(data, 0, len);
							size = size - len;
						}
					}

					len = dis.readInt();
					Log.i(TAG, "len的大小为" + len);
					byte[] data = new byte[len];
					while (len > 0 && (len2 = dis.read(data, 0, len)) != -1) {
						System.out.println("-------len2------->" + len2);
						baos.write(data, 0, len2);
						len = len - len2;
					}
					if (framebuf.size() > 25) {
						try {
							sleep(800);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					Log.i(TAG, "-------------------------------------------");
					setOrget(1, baos.toByteArray());
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void destroy() {
			// TODO Auto-generated method stub
			h264.releaseDecoder();
			try {
				if (ss != null) {
					ss.close();
				}
				if (s != null) {
					s.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.exit(0);
			super.destroy();
		}
	}

	/**
	 * 读取文件类
	 * 
	 * @author Administrator
	 * 
	 */
	public class ReadFileThread extends Thread {
		int file_index = 0;
		File file = new File("/mnt/sdcard/Video.H264");

		// File file = new File("/mnt/sdcard/test.h264");
		@Override
		public void run() {
			try {
				while (file_index < file.length()) {
					byte[] data = new byte[1024 * 50];
					RandomAccessFile raf = new RandomAccessFile(file, "r");
					int len = readOneFrame(raf, data);
					Log.i(TAG, "一帧长度为:" + len);
					byte[] newData = new byte[len];
					System.arraycopy(data, 0, newData, 0, len);
					// Log.i(TAG,
					// "前四个字节为："+newData[0]+" "+newData[1]+" "+newData[2]+" "+newData[3]);

					System.out.println(Arrays.toString(newData));

					setOrget(1, newData);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/**
		 * 读取一帧长度
		 * 
		 * @param raf
		 *            读取文件流
		 * @param data
		 *            保存读取的数据
		 * @return 返回一帧长度
		 * @throws IOException
		 */
		private int readOneFrame(RandomAccessFile raf, byte[] data)
				throws IOException {
			int len = 0;// 表示一帧长度
			raf.seek(file_index);
			while (true) {
				if ((data[len++] = raf.readByte()) == 0
						&& (data[len++] = raf.readByte()) == 0) {
					if ((data[len++] = raf.readByte()) < 2 && len > 6) {
						if (data[len - 1] == 0) {
							if ((data[len++] = raf.readByte()) == 1) {
								file_index += (len - 4);
								return len - 4;
							} else {
								continue;
							}
						} else {
							file_index += (len - 3);
							return len - 3;
						}
					} else {
						continue;
					}
				} else {
					continue;
				}
			}
		}
	}

	/**
	 * 保存或获取数据
	 * 
	 * @param type
	 * @param data
	 * @return
	 */
	public synchronized byte[] setOrget(int type, byte[] data) {
		switch (type) {
		case 1: // 放入数据
			framebuf.add(data);
			return null;
		case 0: // 获取数据
			if (framebuf.size() > 0) {
				byte[] b = framebuf.get(0);
				framebuf.remove(0);
				return b;
			}
		}
		return null;
	}

	/**
	 * 解码线程类
	 * 
	 * @author Administrator
	 * 
	 */
	class DecordeThread implements Runnable {
		@Override
		public void run() {
			while (true) {
				byte[] dataa = setOrget(0, null);
				if (dataa != null && dataa.length > 0) {// 一帧数据收到解码
					int resout = h264.dalDecoder(dataa, dataa.length, mPixel);
					if (resout > 0) {
						Bitmap videoBit = Bitmap.createBitmap(352, 288,
								Config.RGB_565);
						videoBit.copyPixelsFromBuffer(buffer);
						Util.setOrgetBitmap(0, videoBit);
						Log.i(TAG, "集合中的数据：" + Util.list.size());
					}
				}
			}
		}
	}

	public static String nowTime() {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(new Date().getTime());
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		return dateFormat.format(c.getTime());
	}
}
