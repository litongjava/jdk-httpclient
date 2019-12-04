package com.bill.utils.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 接口调用工具类 目前支持 GET POST请求两种方式
 */
public class JdkHttpClient {
  private static URL initUrl(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static URL initUrl(String url, Map<String, String> params) {
    StringBuffer urlParam = buildHttpQueryParams(params);
    try {
      return new URL(url + (urlParam.length() > 0 ? "?" + urlParam.toString() : ""));
    } catch (MalformedURLException e1) {
      e1.printStackTrace();
    }
    return null;
  }

  @SuppressWarnings("deprecation")
  public static StringBuffer buildHttpQueryParams(Map<String, String> params) {
    StringBuffer sb = new StringBuffer();
    for (Entry<String, String> e : params.entrySet()) {
      sb.append(e.getKey() + "=" + URLEncoder.encode(e.getValue().toString()) + "&");
    }
    if (sb.length() > 0) {
      sb.deleteCharAt(sb.lastIndexOf("&"));
    }
    return sb;
  }

  public static HttpURLConnection initHttpUrl(String url, Map<String, String> headers, Map<String, String> params) {
    URL realURL = null;

    if (params != null) {
      realURL = initUrl(url, params);
    } else {
      realURL = initUrl(url);
    }
    HttpURLConnection httpConnection = null;
    try {
      httpConnection = (HttpURLConnection) realURL.openConnection();
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    if (headers != null) {
      for (Map.Entry<String, String> e : headers.entrySet()) {
        httpConnection.setRequestProperty(e.getKey(), e.getValue());
      }
    }
    return httpConnection;
  }

  private static String getFailMessage(HttpURLConnection conn) {
    try {
      return "code : " + conn.getResponseCode() + " url:" + conn.getURL().toString();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static StringBuffer getResponseString(URLConnection conn) {
    // 定义BufferedReader输入流来读取URL的响应
    BufferedReader in = null;
    StringBuffer strBuf = new StringBuffer();
    String line;
    try {
      InputStreamReader inputStreamReader = new InputStreamReader(conn.getInputStream());
      in = new BufferedReader(inputStreamReader);
      while ((line = in.readLine()) != null) {
        strBuf.append(line);
        strBuf.append("\r\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      close(in);
    }
    return strBuf;
  }

  public static void close(AutoCloseable io) {
    if (io != null) {
      try {
        io.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * get请求
   * 
   * @param url
   * @param param
   * @return
   */
  public static StringBuffer get(String url, Map<String, String> headers, Map<String, String> params) {
    // 初始化请求
    HttpURLConnection httpConnection = initHttpUrl(url, headers, params);
    // 设置请求方法
    try {
      httpConnection.setRequestMethod("GET");
    } catch (ProtocolException e1) {
      e1.printStackTrace();
    }
    // 读取数据
    StringBuffer sb = null;
    try {
      if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        String msg = getFailMessage(httpConnection);
        throw new RuntimeException("HTTP GET Request Failed with Error " + msg);
      } else {
        sb = getResponseString(httpConnection);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      httpConnection.disconnect();
    }
    return sb;
  }

  /**
   * 向指定URL发送post请求
   */
  public static String post(String url, Map<String, String> headers, Map<String, String> params) {
    HttpURLConnection httpConn = postStream(url, headers, params);

    StringBuffer sb = null;
    try {
      // 读取响应
      if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
        sb = getResponseString(httpConn);
      } else {
        throw new RuntimeException("HTTP POST Request Failed with Error " + getFailMessage(httpConn));
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      httpConn.disconnect();// 断开连接
    }
    return sb.toString();
  }

  /**
   * 返回HttpURLConnection
   * 自行从HttpURLConnection中获取流进行保存
   * 
   */
  public static HttpURLConnection postStream(String url, Map<String, String> headers, Map<String, String> params) {
    StringBuffer urlParam = buildHttpQueryParams(params);
    HttpURLConnection httpConn = initHttpUrl(url, headers, null);

    // 设置参数
    httpConn.setDoOutput(true); // 需要输出
    httpConn.setDoInput(true); // 需要输入
    httpConn.setUseCaches(false); // 不允许缓存
    try {
      httpConn.setRequestMethod("POST");
    } catch (ProtocolException e1) {
      e1.printStackTrace();
    } // 设置POST方式连接
    httpConn.setInstanceFollowRedirects(true); // 设置跟随重定向
    // 连接,也可以不用明文connect，使用下面的httpConn.getOutputStream()会自动connect
    // httpConn.connect();

    DataOutputStream dos = null;
    try {
      // 建立输入流，向指向的URL传入参数 ,支持的格式是 key=value&key=value
      dos = new DataOutputStream(httpConn.getOutputStream());
      dos.writeBytes(urlParam.toString());
      dos.flush();
    } catch (IOException e1) {
      e1.printStackTrace();
    } finally {
      close(dos);
    }
    return httpConn;
  }
}
