package wechat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.jdom2.CDATA;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

public class PushManage {
	public String PushManageXml(InputStream is) throws Exception {

		String returnStr = ""; // 反回Servlet字符串
		String toName = ""; // 开发者微信号
		String fromName = ""; // 发送方帐号（一个OpenID）
		String type = ""; // 请求类型
		String con = ""; // 消息内容(接收)
		String event = ""; // 自定义按钮事件请求
		String eKey = ""; // 事件请求key值
		String voiceUTF8 = "";
		String autoReplyStr = "欢迎关注，现在正在测试开发模式。\n尝试输入'UVI', 系统会联系NEA(National Environment Agency)获取当日UVI指数。\n尝试使用语音测试识别结果。";

		try {

			SAXBuilder sax = new SAXBuilder();
			Document doc = sax.build(is);
			// 获得文件的根元素
			Element root = doc.getRootElement();

			// 获得根元素的第一级子节点
			List<Element> list = root.getChildren();

			for (int j = 0; j < list.size(); j++) {
				// 获得结点
				Element first = (Element) list.get(j);

				String elementName = first.getName();
				String elementValue = first.getValue().trim();

				if (elementName.equals("ToUserName")) {
					toName = elementValue;
				} else if (elementName.equals("FromUserName")) {
					fromName = elementValue;
				} else if (elementName.equals("MsgType")) {
					type = elementValue;
				} else if (elementName.equals("Content")) {
					con = elementValue;
				} else if (elementName.equals("Event")) {
					event = elementValue;
				} else if (elementName.equals("EventKey")) {
					eKey = elementValue;
				} else if (elementName.equals("Recognition")) {
					voiceUTF8 = elementValue;
				}
			}

		} catch (IOException e) {
			System.out.println(e.toString());
		}

		if (type.equals("event")) { // 此为事件
			if (event.equals("subscribe")) {// 此为 关注事件
				returnStr = getBackXMLTypeText(toName, fromName, autoReplyStr);
			}
			if (event.equals("unsubscribe")) { // 此为取消关注事件
			}
			if (event.equals("CLICK")) { // 此为 自定义菜单按钮点击事件
				if (eKey.equals("V1")) {
					getBackXMLTypeText(toName, fromName, "点击了菜单1");
				}
				if (eKey.equals("V2")) {
					getBackXMLTypeText(toName, fromName, "点击了菜单2");
				}
			}
		}
		if (type.equals("text")) {

			if (con.toLowerCase().contains("nea")) {
				String uvi = "http://www.nea.gov.sg/api/WebAPI?dataset=uvi&keyref=781CF461BB6606ADEA6B1B4F3228DE9DC0C6CE9E76A92CAD";
				String nowcast = "http://www.nea.gov.sg/api/WebAPI?dataset=nowcast&keyref=781CF461BB6606ADEA6B1B4F3228DE9DC0C6CE9E76A92CAD";
				String responseStr = "";
				String content = "Did not find NEA related infomation";
				String queryUri = "";

				if (con.toLowerCase().contains("uvi")) {
					queryUri = uvi;
				}
				if (con.toLowerCase().contains("now")) {
					queryUri = nowcast;
				}

				if (queryUri != "") {
					InputStream inStream = GetReposeStream(queryUri);
					responseStr = readStream(inStream);
				}

				if (con.toLowerCase().contains("uvi")) {

				}
				if (con.toLowerCase().contains("now")) {

					SAXBuilder saxBuilder = new SAXBuilder();
					try {
						Document doc = saxBuilder.build(new StringReader(responseStr));
						Element root = doc.getRootElement();

						String title = root.getChild("title").getValue();
						String source = root.getChild("source").getValue();
						String description = root.getChild("description").getValue();
						String forecast = "";
						String areaName = "";
						String foundArea = "";

						Element item = root.getChild("item");
						String issueDateTime = item.getChild("issue_datentime").getValue();
						Element weatherForecastElement = item.getChild("weatherForecast");
						List<Element> allAreaItem = weatherForecastElement.getChildren();

						for (Element area : allAreaItem) {
							areaName = area.getAttribute("name").getValue();

							if (con.toUpperCase().contains(areaName)) {
								forecast = area.getAttribute("forecast").getValue();
								foundArea = areaName;
								break;
							}
						}

						content = "";
						content += "Title: " + title;
						content += "\nSouce: " + source;
						content += "\nIssue date time:" + issueDateTime;
						content += "\nArea name: " + areaName;
						content += "\nForcast: " + forecast;

					} catch (JDOMException e) {
						System.out.println(e.getMessage());
					} catch (IOException e) {
						System.out.println(e.getMessage());
					}

				}

				returnStr = getBackXMLTypeText(toName, fromName, content);

			} else {
				returnStr = getBackXMLTypeText(toName, fromName, "谢谢，暂时没有关于 ' " + con + " ' 的智能搜索结果。我们正在建设\n" + autoReplyStr);
			}

		}
		if (type.equals("voice")) {
			returnStr = getBackXMLTypeText(toName, fromName, "欢迎测试语音识别，识别结果： " + voiceUTF8);
		}

		return returnStr;
	}

	private InputStream GetReposeStream(String uri) throws Exception {

		URL obj = new URL(uri);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");

		int responseCode = con.getResponseCode();
		if (responseCode == 200) {
			return con.getInputStream();
		} else {
			System.out.println("Error in accessing API - " + readStream(con.getErrorStream()));
			return null;
		}

	}

	private String readStream(InputStream inputStream) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = reader.readLine()) != null) {
			response.append(inputLine);
		}
		reader.close();
		return response.toString();
	}

	/**
	 * 编译文本信息
	 */
	private String getBackXMLTypeText(String toName, String fromName, String content) throws IOException {

		String returnStr = "";

		Long time = new Date().getTime();

		Element rootXML = new Element("xml");
		rootXML.addContent(new Element("ToUserName").setContent(new CDATA(fromName)));
		rootXML.addContent(new Element("FromUserName").setContent(new CDATA(toName)));
		rootXML.addContent(new Element("CreateTime").setText(time.toString()));
		rootXML.addContent(new Element("MsgType").setContent(new CDATA("text")));
		rootXML.addContent(new Element("Content").setContent(new CDATA(content)));

		Document doc = new Document(rootXML);
		XMLOutputter XMLOut = new XMLOutputter();
		returnStr = XMLOut.outputString(doc);

		return returnStr;
	}

	/**
	 * 编译图片信息(单图模式)
	 */
	private String getBackXMLTypeImg(String toName, String fromName, String title, String content, String url, String pUrl) {

		String returnStr = "";

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
		String times = format.format(new Date());

		Element rootXML = new Element("xml");

		rootXML.addContent(new Element("ToUserName").setText(fromName));
		rootXML.addContent(new Element("FromUserName").setText(toName));
		rootXML.addContent(new Element("CreateTime").setText(times));
		rootXML.addContent(new Element("MsgType").setText("news"));
		rootXML.addContent(new Element("ArticleCount").setText("1"));

		Element fXML = new Element("Articles");
		Element mXML = null;

		mXML = new Element("item");
		mXML.addContent(new Element("Title").setText(title));
		mXML.addContent(new Element("Description").setText(content));
		mXML.addContent(new Element("PicUrl").setText(pUrl));
		mXML.addContent(new Element("Url").setText(url));
		fXML.addContent(mXML);
		rootXML.addContent(fXML);

		Document doc = new Document(rootXML);

		XMLOutputter XMLOut = new XMLOutputter();
		returnStr = XMLOut.outputString(doc);

		return returnStr;
	}

	/**
	 * 编译图片信息(无图模式)
	 */
	private String getBackXMLTypeImg(String toName, String fromName, String title, String content, String url) {

		String returnStr = "";

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
		String times = format.format(new Date());

		Element rootXML = new Element("xml");

		rootXML.addContent(new Element("ToUserName").setText(fromName));
		rootXML.addContent(new Element("FromUserName").setText(toName));
		rootXML.addContent(new Element("CreateTime").setText(times));
		rootXML.addContent(new Element("MsgType").setText("news"));
		rootXML.addContent(new Element("ArticleCount").setText("1"));

		Element fXML = new Element("Articles");
		Element mXML = null;

		// String url = "";
		String ss = "";
		mXML = new Element("item");
		mXML.addContent(new Element("Title").setText(title));
		mXML.addContent(new Element("Description").setText(content));
		mXML.addContent(new Element("PicUrl").setText(ss));
		mXML.addContent(new Element("Url").setText(url));
		fXML.addContent(mXML);
		rootXML.addContent(fXML);

		Document doc = new Document(rootXML);

		XMLOutputter XMLOut = new XMLOutputter();
		returnStr = XMLOut.outputString(doc);

		return returnStr;
	}

	/**
	 * 编译音乐信息
	 */
	@SuppressWarnings("unused")
	private String getBackXMLTypeMusic(String toName, String fromName, String content) {

		String returnStr = "";

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
		String times = format.format(new Date());

		Element rootXML = new Element("xml");

		rootXML.addContent(new Element("ToUserName").setText(fromName));
		rootXML.addContent(new Element("FromUserName").setText(toName));
		rootXML.addContent(new Element("CreateTime").setText(times));
		rootXML.addContent(new Element("MsgType").setText("music"));

		Element mXML = new Element("Music");

		mXML.addContent(new Element("Title").setText("音乐"));
		mXML.addContent(new Element("Description").setText("音乐让人心情舒畅！"));
		mXML.addContent(new Element("MusicUrl").setText(content));
		mXML.addContent(new Element("HQMusicUrl").setText(content));

		rootXML.addContent(mXML);

		Document doc = new Document(rootXML);

		XMLOutputter XMLOut = new XMLOutputter();
		returnStr = XMLOut.outputString(doc);

		return returnStr;
	}
}