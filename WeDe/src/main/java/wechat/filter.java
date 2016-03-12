package wechat;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jdom2.JDOMException;

public class filter implements Filter {

	// 这个Token是给微信开发者接入时填的
	// 可以是任意英文字母或数字，长度为3-32字符
	private static String Token = "ads23gacwsd67890123";

	public void init(FilterConfig config) throws ServletException {
		System.out.println("WeixinUrlFilter successfully started");
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		// 微信服务器将发送GET请求到填写的URL上,这里需要判定是否为GET请求
		boolean isGet = request.getMethod().toLowerCase().equals("get");
		System.out.println("Got wechat request:" + request.getMethod() + "method");
		if (isGet) {
			// 验证URL真实性
			String signature = request.getParameter("signature");// 微信加密签名
			String timestamp = request.getParameter("timestamp");// 时间戳
			String nonce = request.getParameter("nonce");// 随机数
			String echostr = request.getParameter("echostr");// 随机字符串
			List<String> params = new ArrayList<String>();
			params.add(Token);
			params.add(timestamp);
			params.add(nonce);
			// 1. 将token、timestamp、nonce三个参数进行字典序排序
			Collections.sort(params, new Comparator<String>() {
				public int compare(String o1, String o2) {
					return o1.compareTo(o2);
				}
			});
			// 2. 将三个参数字符串拼接成一个字符串进行sha1加密
			String temp = SHA1.encode(params.get(0) + params.get(1) + params.get(2));
			if (temp.equals(signature)) {
				response.getWriter().write(echostr);
			}
		} else {
			// deal with post
			response.setCharacterEncoding("UTF-8");
			request.setCharacterEncoding("UTF-8");
			PrintWriter out = response.getWriter();
			try {
				InputStream is = request.getInputStream();
				PushManage push = new PushManage();
				String getXml = push.PushManageXml(is);
				out.print(getXml); // print is better, check difference
				// out.write(getXml);

				System.out.println("the PushManager return string" + getXml);

			} catch (JDOMException e) {
				e.printStackTrace();
				out.print("");
			} catch (Exception e) {
				out.print(e.toString());
			} finally {
				if (out != null) {
					out.flush();
					out.close();
				}
			}
		}
	}

	public void destroy() {

	}
}