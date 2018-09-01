package com.yzt.logic.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 常量
 */
public class Cnst {
	
	public final static String PING_MESSAGE = "ping";
	
	// 获取项目版本信息
    public static final String version = ProjectInfoPropertyUtil.getProperty("project_version", "1.5");
    public static Boolean isTest = true;//是否是测试环境


    public static final String p_name = ProjectInfoPropertyUtil.getProperty("p_name", "wsw_X13");
    public static final String o_name = ProjectInfoPropertyUtil.getProperty("o_name", "u_consume");
    public static final String gm_url = ProjectInfoPropertyUtil.getProperty("gm_url", "");
    
    //回放配置
    public static final String BACK_FILE_PATH = ProjectInfoPropertyUtil.getProperty("backFilePath", "1.5");
    public static final String FILE_ROOT_PATH = ProjectInfoPropertyUtil.getProperty("fileRootPath", "1.5");
    public static String SERVER_IP = getLocalAddress();
    public static String HTTP_URL = "http://".concat(Cnst.SERVER_IP).concat(":").concat(ProjectInfoPropertyUtil.getProperty("httpUrlPort", "8086")).concat("/");
    public static String getLocalAddress(){
		String ip = "";
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ip;
	}
    
    
    public static final String cid = ProjectInfoPropertyUtil.getProperty("cid", "13");;
    //redis配置
    public static final String REDIS_HOST = ProjectInfoPropertyUtil.getProperty("redis.host", "");
    public static final String REDIS_PORT = ProjectInfoPropertyUtil.getProperty("redis.port", "");
    public static final String REDIS_PASSWORD = ProjectInfoPropertyUtil.getProperty("redis.password", "");

    //mina的端口
    public static final String MINA_PORT = ProjectInfoPropertyUtil.getProperty("mina.port", "");
    
    //用户服务地址
    public static final String GETUSER_URL = ProjectInfoPropertyUtil.getProperty("getUser_url","");
    //mina
    public static final int Session_Read_BufferSize = 2048 * 10;
    public static final int Session_life = 60;
    public static final int WriteTimeOut = 500;
    
    public static final String rootPath = ProjectInfoPropertyUtil.getProperty("rootPath", "");

    public static final long HEART_TIME = 9000;//心跳时间，前端定义为8s，避免网络问题延时，后端计算是以9s计算
    public static final int MONEY_INIT = 4;//初始赠送给用户的房卡数
    //开房选项中的是否
    public static final int YES = 1;
    public static final int NO = 0;
    

    public static final long ROOM_OVER_TIME = 5*60*60*1000;//房间定时5小时解散
    public static final long ROOM_CREATE_DIS_TIME = 40*60*1000;//创建房间之后，40分钟解散
    public static final long ROOM_DIS_TIME = 5*60*1000;//玩家发起解散房间之后，5分钟自动解散
    public static final String CLEAN_3 = "0 0 3 * * ?";
    public static final String CLEAN_EVERY_HOUR = "0 0 0/1 * * ?";
    public static final String COUNT_EVERY_TEN_MINUTE = "0 0/1 * * * ?";
    public static final long BACKFILE_STORE_TIME = 3*24*60*60*1000;//回放文件保存时间
   
    
    //玩家数据更新间隔时间
    public static long updateDiffTime =3*24*3600*1000;
    //测试时间
//    public static final long ROOM_OVER_TIME = 60*1000;//
//    public static final long ROOM_CREATE_DIS_TIME = 20*1000;
//    public static final long ROOM_DIS_TIME = 10*1000;
//	public static final String CLEAN_3 = "0/5 * * * * ?";
//	public static final String CLEAN_EVERY_HOUR = "0/5 * * * * ?";
//    public static final String COUNT_EVERY_TEN_MINUTE = "0/1 * * * * ?";
//    public static final long BACKFILE_STORE_TIME = 60*1000;//回放文件保存时间
//    public static final int PLAYOVER_LIFE_TIME = 60;//战绩保存时间
    

    public static final int ROOM_LIFE_TIME_CREAT = (int) ((ROOM_OVER_TIME/1000)+200);//创建时，5小时，redis用
    public static final int ROOM_LIFE_TIME_DIS = (int) ((ROOM_DIS_TIME/1000)+200);//解散房间时，300s，redis用
    public static final int ROOM_LIFE_TIME_COMMON = (int) ((ROOM_CREATE_DIS_TIME/1000)+200);//正常开局存活时间，redis用
    public static final int OVERINFO_LIFE_TIME_COMMON = (int) (10*60);//大结算 overInfo 存活时间
    public static final int PLAYOVER_LIFE_TIME =3*24*60*60;//战绩保存时间
    public static final int HUIFANG_LIFE_TIME = 30*60;//30分钟 回放写入文件后删除
    
    public static final int DIS_ROOM_RESULT = 1;

    public static final int DIS_ROOM_TYPE_1 = 1;//创建房间40分钟解散类型
    public static final int DIS_ROOM_TYPE_2 = 2;//玩家点击解散房间类型

    public static final int PAGE_SIZE = 10;
    



    public static final String USER_SESSION_USER_ID = "user_id";
    public static final String USER_SESSION_IP = "ip";
    
    

    //房间信息中的state
    // 1等待玩家入坐；2游戏中；3小结算  4大结算或已解散
    public static final int ROOM_STATE_CREATED = 1;
    public static final int ROOM_STATE_GAMIING = 2;
    public static final int ROOM_STATE_XJS = 3;
    public static final int ROOM_STATE_YJS = 4;


    //房间类型
    public static final int ROOM_TYPE_1 = 1;//房主模式
    public static final int ROOM_TYPE_2 = 2;//自由模式
    public static final int ROOM_TYPE_3 = 3;//AA

    //风向表示
    public static final int WIND_EAST = 1;//东
    public static final int WIND_SOUTH = 2;//南
    public static final int WIND_WEST = 3;//西
    public static final int WIND_NORTH = 4;//北

    //开房的局数对应消耗的房卡数
    public static final Map<Integer,Integer> moneyMap = new HashMap<>();
    static {
        moneyMap.put(2,4);
        moneyMap.put(4,6);
        moneyMap.put(8,12);
    }
    //玩家在线状态 state 
    public static final int PLAYER_LINE_STATE_INLINE = 1;//"inline"
    public static final int PLAYER_LINE_STATE_OUT = 2;//"out"
    
    //玩家进入或退出代开房间
    public static final int PLAYER_EXTRATYPE_ADDROOM = 1;//进入
    public static final int PLAYER_EXTRATYPE_EXITROOM = 2;//退出
    public static final int PLAYER_EXTRATYPE_JIESANROOM = 3;//解散
    public static final int PLAYER_EXTRATYPE_LIXIAN = 4;//离线
    public static final int PLAYER_EXTRATYPE_SHANGXIAN = 5;//上线
    public static final int PLAYER_EXTRATYPE_KAIJU = 6;//房间开局
    //玩家状态
    public static final int PLAYER_STATE_DATING = 1;//"dating"
    public static final int PLAYER_STATE_IN = 2;//"in"
    public static final int PLAYER_STATE_PREPARED = 3;//"prepared"
    public static final int PLAYER_STATE_GAME = 4;//"game"
    public static final int PLAYER_STATE_XJS = 5;//"xjs"
    

    //请求状态
    public static final int REQ_STATE_FUYI = -1;//敬请期待
    public static final int REQ_STATE_0 = 0;//非法请求
    public static final int REQ_STATE_1 = 1;//正常
    public static final int REQ_STATE_2 = 2;//余额不足
    public static final int REQ_STATE_3 = 3;//已经在其他房间中
    public static final int REQ_STATE_4 = 4;//房间不存在
    public static final int REQ_STATE_5 = 5;//房间人员已满
    public static final int REQ_STATE_6 = 6;//游戏中，不能退出房间
    public static final int REQ_STATE_7 = 7;//有玩家拒绝解散房间
    public static final int REQ_STATE_8 = 8;//玩家不存在（代开模式中，房主踢人用的）
    public static final int REQ_STATE_9 = 9;//接口id不符合，需请求大接口
    public static final int REQ_STATE_10 = 10;//代开房间创建成功
    public static final int REQ_STATE_11 = 11;//已经代开过10个了，不能再代开了
    public static final int REQ_STATE_12 = 12;//房间存在超过24小时解散的提示
    public static final int REQ_STATE_13 = 13;//房间40分钟未开局解散提示
    public static final int REQ_STATE_14 = 14;//ip不一致

    //动作类型
    public static final int ACTION_TYPE_CHI = 1;
    public static final int ACTION_TYPE_PENG = 2;
    public static final int ACTION_TYPE_PENGGANG = 3;
    public static final int ACTION_TYPE_DIANGANG = 4;
    public static final int ACTION_TYPE_ANGANG = 5;
    public static final int ACTION_TYPE_FAPAI = 6;
    public static final int ACTION_TYPE_CHUPAI = 7;
    public static final int ACTION_TYPE_GUO = 8;
    public static final int ACTION_TYPE_HU = 9;
    public static final int ACTION_TYPE_DUIHUN = 10;
    
    //检测类型
    public static final int CHECK_TYPE_ZIJIMO = 1;
    public static final int CHECK_TYPE_BIERENCHU = 2;
    public static final int CHECK_TYPE_HAIDIANPAI = 3;
    
    //退出类型
    public static final int EXIST_TYPE_EXIST = 1;//"exist"
    public static final int EXIST_TYPE_DISSOLVE = 2;//"dissolve";

    // 项目根路径
    public static String ROOTPATH = "";
    
    //redis存储的key的不同类型的前缀
    public static final String REDIS_PREFIX_ROOMMAP = "TJMJ_ROOM_MAP_";//房间信息
    public static final String REDIS_PREFIX_OPENIDUSERMAP = "TJMJ_OPENID_USERID_MAP_";//openId-user数据
    public static final String REDIS_PREFIX_USER_ID_USER_MAP = "TJMJ_USER_ID_USER_MAP_";//通过userId获取用户
    
    //代开房间列表
    public static final String REDIS_PREFIX_DAI_ROOM_LIST = "TJMJ_DAIKAI_ROOM_LIST_";//通过代开房间列表
    //redis中通知的key
    public static final String NOTICE_KEY = "TJMJ_NOTICE_KEY";
    
    public static final String PROJECT_PREFIX = "TJMJ_*";
    
    public static final String REDIS_ONLINE_NUM_COUNT = "TJMJ_ONLINE_NUM_";
    
    public static final String REDIS_HEART_PREFIX = "TJMJ_HEART_USERS_MAP";
    
    public static final String REDIS_HUIFANG_MAP = "TJMJ_HUIFANG_MAP_";//房间号+时间戳+小局
    
    
    //俱乐部
    public static final String REDIS_CLUB_CLUBMAP = "TJMJ_CLUB_MAP_";//俱乐部信息
    public static final String REDIS_CLUB_ROOM_LIST = "TJMJ_CLUB_MAP_LIST_";//存放俱乐部未开局房间信息
    public static final String REDIS_CLUB_PLAY_RECORD_PREFIX = "TJMJ_CLUB_PLAY_RECORD_";//房间战绩
    public static final String REDIS_CLUB_PLAY_RECORD_PREFIX_ROE_USER = "TJMJ_CLUB_PLAY_RECORD_FOR_USER_";//玩家字段
    public static final String REDIS_CLUB_TODAYSCORE_ROE_USER = "CLUB_TJMJ_TODAYSCORE_FOR_USER_";//统计玩家今日分数
    public static final String REDIS_CLUB_TODAYJUNUM_ROE_USER = "CLUB_TJMJ_TODAYJUNUM_FOR_USER_";//统计玩家今日局数
    public static final String REDIS_CLUB_ACTIVE_NUM = "CLUB_TJMJ_ACTIVE_NUM_";//今天活跃人数
    public static final String REDIS_CLUB_TODAYKAI_NUM = "CLUB_TJMJ_TODAYKAI_NUM_";//今天开局数
    public static final int REDIS_CLUB_DIE_TIME = 3*24*60*60;//玩家战绩和俱乐部每天活跃保存时间
    public static final int REDIS_CLUB_PLAYERJUNUM_TIME =3* 24*60*60;//玩家今日局数和昨日局数保存时间
    //这个字段不清理，存放玩家战绩，定时任务定期清理内容
    public static final String REDIS_PLAY_RECORD_PREFIX = "TJMJ_PLAY_RECORD_";//房间战绩
    public static final String REDIS_PLAY_RECORD_PREFIX_ROE_USER = "TJMJ_PLAY_RECORD_FOR_USER_";//玩家字段
    public static final String REDIS_PLAY_RECORD_PREFIX_ROE_DAIKAI = "TJMJ_PLAY_RECORD_FOR_DAIKAI_";//代开房间
    public static final String REDIS_PLAY_RECORD_PREFIX_OVERINFO = "TJMJ_PLAY_RECORD_OVERINFO_";//大结算
    
   
    public static Map<String,String> ROUTE_MAP = new ConcurrentHashMap<String, String>();
    static{
    	ROUTE_MAP.put("a","interfaceId");
    	ROUTE_MAP.put("b","state");
    	ROUTE_MAP.put("c","message");
    	ROUTE_MAP.put("d","info");
    	ROUTE_MAP.put("e","others");
    	ROUTE_MAP.put("f","page");
    	ROUTE_MAP.put("g","infos");
    	ROUTE_MAP.put("h","pages");
    	ROUTE_MAP.put("i","connectionInfo");
    	ROUTE_MAP.put("j","help");
    	ROUTE_MAP.put("k","userId");
    	ROUTE_MAP.put("l","content");
    	ROUTE_MAP.put("m","tel");
    	ROUTE_MAP.put("n","roomType");
    	ROUTE_MAP.put("o","type");
    	ROUTE_MAP.put("p","clubId");
    	ROUTE_MAP.put("q","clubName");
    	ROUTE_MAP.put("r","clubUserName");
    	ROUTE_MAP.put("s","allNums");
    	ROUTE_MAP.put("t","maxNums");
    	ROUTE_MAP.put("u","freeStart");
    	ROUTE_MAP.put("v","freeEnd");
    	ROUTE_MAP.put("w","clubMoney");
    	ROUTE_MAP.put("x","cardQuota");
    	ROUTE_MAP.put("y","juNum");
    	ROUTE_MAP.put("z","roomSn");
    	ROUTE_MAP.put("A","roomId");
    	ROUTE_MAP.put("B","reqState");
    	ROUTE_MAP.put("C","playerNum");
    	ROUTE_MAP.put("D","money");
    	ROUTE_MAP.put("E","playStatus");
    	ROUTE_MAP.put("F","position");
    	ROUTE_MAP.put("G","userInfo");
    	ROUTE_MAP.put("H","wsw_sole_main_id");
    	ROUTE_MAP.put("I","wsw_sole_action_id");
    	ROUTE_MAP.put("J","roomInfo");
    	ROUTE_MAP.put("K","lastNum");
    	ROUTE_MAP.put("L","totalNum");
    	ROUTE_MAP.put("M","roomIp");
    	ROUTE_MAP.put("N","ip");
    	ROUTE_MAP.put("O","xjst");
    	ROUTE_MAP.put("P","score");
    	ROUTE_MAP.put("Q","userName");
    	ROUTE_MAP.put("R","userImg");
    	ROUTE_MAP.put("S","joinIndex");
    	ROUTE_MAP.put("T","gender");
    	ROUTE_MAP.put("U","createTime");
    	ROUTE_MAP.put("V","circleNum");
    	ROUTE_MAP.put("W","playerInfo");
    	ROUTE_MAP.put("X","openId");
    	ROUTE_MAP.put("Y","cId");
    	ROUTE_MAP.put("Z","currentUser");
    	ROUTE_MAP.put("aa","anotherUsers");
    	ROUTE_MAP.put("ab","version");
    	ROUTE_MAP.put("ac","userAgree");
    	ROUTE_MAP.put("ad","notice");
    	ROUTE_MAP.put("ae","actNum");
    	ROUTE_MAP.put("af","exState");
    	ROUTE_MAP.put("ag","pais");
    	ROUTE_MAP.put("ah","xiaoJuNum");
    	ROUTE_MAP.put("ai","zhuangPlayer");
    	ROUTE_MAP.put("aj","dissolveTime");
    	ROUTE_MAP.put("ak","othersAgree");
    	ROUTE_MAP.put("al","dissolveRoom");
    	ROUTE_MAP.put("am","continue");
    	ROUTE_MAP.put("an","nextAction");
    	ROUTE_MAP.put("ao","nextActionUserId");
    	ROUTE_MAP.put("ap","agree");
    	ROUTE_MAP.put("aq","idx");
    	ROUTE_MAP.put("ar", "rule");
    	ROUTE_MAP.put("as", "finalScore");
    	ROUTE_MAP.put("at", "date");
    	ROUTE_MAP.put("au", "extra");
    	ROUTE_MAP.put("av", "extraType");
    	
    	ROUTE_MAP.put("aw", "scoreType");
    	ROUTE_MAP.put("ax", "chuList");
    	ROUTE_MAP.put("ay", "duiNum");
    	ROUTE_MAP.put("az", "actionList");
    	ROUTE_MAP.put("ba", "currMJNum");
    	ROUTE_MAP.put("bb", "hunPai");
    	ROUTE_MAP.put("bc", "lastChuPai");
    	ROUTE_MAP.put("bd", "lastChuPaiUserId");
    	ROUTE_MAP.put("be", "currUserId");
    	ROUTE_MAP.put("bf", "currAction");
    	ROUTE_MAP.put("bg", "action");
    	ROUTE_MAP.put("bh", "needFaPaiUserId");
    	ROUTE_MAP.put("bi", "zhuangNum");
    	ROUTE_MAP.put("bj", "pengPengHu");
    	ROUTE_MAP.put("bk", "sanJiaQing");
    	ROUTE_MAP.put("bl", "qingYiSe");
    	ROUTE_MAP.put("bm", "huNum");
    	ROUTE_MAP.put("bn", "dianNum");
    	ROUTE_MAP.put("bo", "isWin");
    	ROUTE_MAP.put("bp", "isDian");
    	ROUTE_MAP.put("bq", "winInfo");
    	ROUTE_MAP.put("br", "dingHunPai");
    	ROUTE_MAP.put("bs", "isCreated");
    	ROUTE_MAP.put("bt", "lastFaPai");
    	ROUTE_MAP.put("bu", "toUserId");
    	ROUTE_MAP.put("bv", "startPosition");
    	
    	//俱乐部信息
    	ROUTE_MAP.put("bw", "used");
    	ROUTE_MAP.put("bx", "users");
    	ROUTE_MAP.put("by", "rooms");

    	//////////////////////////////////////////////////////
    	ROUTE_MAP.put("interfaceId","a");
    	ROUTE_MAP.put("state","b");
    	ROUTE_MAP.put("message","c");
    	ROUTE_MAP.put("info","d");
    	ROUTE_MAP.put("others","e");
    	ROUTE_MAP.put("page","f");
    	ROUTE_MAP.put("infos","g");
    	ROUTE_MAP.put("pages","h");
    	ROUTE_MAP.put("connectionInfo","i");
    	ROUTE_MAP.put("help","j");
    	ROUTE_MAP.put("userId","k");
    	ROUTE_MAP.put("content","l");
    	ROUTE_MAP.put("tel","m");
    	ROUTE_MAP.put("roomType","n");
    	ROUTE_MAP.put("type","o");
    	ROUTE_MAP.put("clubId","p");
    	ROUTE_MAP.put("clubName","q");
    	ROUTE_MAP.put("clubUserName","r");
    	ROUTE_MAP.put("allNums","s");
    	ROUTE_MAP.put("maxNums","t");
    	ROUTE_MAP.put("freeStart","u");
    	ROUTE_MAP.put("freeEnd","v");
    	ROUTE_MAP.put("clubMoney","w");
    	ROUTE_MAP.put("cardQuota","x");
    	ROUTE_MAP.put("juNum","y");
    	ROUTE_MAP.put("roomSn","z");
    	ROUTE_MAP.put("roomId","A");
    	ROUTE_MAP.put("reqState","B");
    	ROUTE_MAP.put("playerNum","C");
    	ROUTE_MAP.put("money","D");
    	ROUTE_MAP.put("playStatus","E");
    	ROUTE_MAP.put("position","F");
    	ROUTE_MAP.put("userInfo","G");
    	ROUTE_MAP.put("wsw_sole_main_id","H");
    	ROUTE_MAP.put("wsw_sole_action_id","I");
    	ROUTE_MAP.put("roomInfo","J");
    	ROUTE_MAP.put("lastNum","K");
    	ROUTE_MAP.put("totalNum","L");
    	ROUTE_MAP.put("roomIp","M");
    	ROUTE_MAP.put("ip","N");
    	ROUTE_MAP.put("xjst","O");
    	ROUTE_MAP.put("score","P");
    	ROUTE_MAP.put("userName","Q");
    	ROUTE_MAP.put("userImg","R");
    	ROUTE_MAP.put("joinIndex","S");
    	ROUTE_MAP.put("gender","T");
    	ROUTE_MAP.put("createTime","U");
    	ROUTE_MAP.put("circleNum","V");
    	ROUTE_MAP.put("playerInfo","W");
    	ROUTE_MAP.put("openId","X");
    	ROUTE_MAP.put("cId","Y");
    	ROUTE_MAP.put("currentUser","Z");
    	ROUTE_MAP.put("anotherUsers","aa");
    	ROUTE_MAP.put("version","ab");
    	ROUTE_MAP.put("userAgree","ac");
    	ROUTE_MAP.put("notice","ad");
    	ROUTE_MAP.put("actNum","ae");
    	ROUTE_MAP.put("exState","af");
    	ROUTE_MAP.put("pais","ag");
    	ROUTE_MAP.put("xiaoJuNum","ah");
    	ROUTE_MAP.put("zhuangPlayer","ai");
    	ROUTE_MAP.put("dissolveTime","aj");
    	ROUTE_MAP.put("othersAgree","ak");
    	ROUTE_MAP.put("dissolveRoom","al");
    	ROUTE_MAP.put("continue","am");
    	ROUTE_MAP.put("nextAction","an");
    	ROUTE_MAP.put("nextActionUserId","ao");
    	ROUTE_MAP.put("agree","ap");
    	ROUTE_MAP.put("idx","aq");
    	ROUTE_MAP.put("rule", "ar");
    	ROUTE_MAP.put("finalScore", "as");
    	ROUTE_MAP.put("date", "at");
    	ROUTE_MAP.put("extra", "au");
    	ROUTE_MAP.put("extraType", "av");
    	ROUTE_MAP.put("scoreType", "aw");
    	ROUTE_MAP.put("chuList", "ax");
    	ROUTE_MAP.put("duiNum", "ay");
    	ROUTE_MAP.put("actionList", "az");
    	ROUTE_MAP.put("currMJNum", "ba");
    	ROUTE_MAP.put("hunPai", "bb");
    	ROUTE_MAP.put("lastChuPai", "bc");
    	ROUTE_MAP.put("lastChuPaiUserId", "bd");
    	ROUTE_MAP.put("currUserId", "be");
    	ROUTE_MAP.put("currAction", "bf");
    	ROUTE_MAP.put("action", "bg");
    	ROUTE_MAP.put("needFaPaiUserId", "bh");
    	ROUTE_MAP.put("zhuangNum", "bi");
    	ROUTE_MAP.put("pengPengHu", "bj");
    	ROUTE_MAP.put("sanJiaQing", "bk");
    	ROUTE_MAP.put("qingYiSe", "bl");
    	ROUTE_MAP.put("huNum", "bm");
    	ROUTE_MAP.put("dianNum", "bn");
    	ROUTE_MAP.put("isWin", "bo");
    	ROUTE_MAP.put("isDian", "bp");
    	ROUTE_MAP.put("winInfo", "bq");
    	ROUTE_MAP.put("dingHunPai", "br");
    	ROUTE_MAP.put("isCreated", "bs");
    	ROUTE_MAP.put("lastFaPai", "bt");
    	ROUTE_MAP.put("toUserId", "bu");
    	ROUTE_MAP.put("startPosition", "bv");
    	
    	
     	//俱乐部信息
    	ROUTE_MAP.put("used", "bw");
    	ROUTE_MAP.put("users", "bx");
    	ROUTE_MAP.put("rooms", "by");

    }

    
    public static int[] CHI35 = new int[]{1,2,3};
    public static int[] CHI36 = new int[]{2,3,4};
    public static int[] CHI37 = new int[]{3,4,5};
    public static int[] CHI38 = new int[]{4,5,6};
    public static int[] CHI39 = new int[]{5,6,7};
    public static int[] CHI40 = new int[]{6,7,8};
    public static int[] CHI41 = new int[]{7,8,9};
    
    public static int[] CHI42 = new int[]{10,11,12};
    public static int[] CHI43 = new int[]{11,12,13};
    public static int[] CHI44 = new int[]{12,13,14};
    public static int[] CHI45 = new int[]{13,14,15};
    public static int[] CHI46 = new int[]{14,15,16};
    public static int[] CHI47 = new int[]{15,16,17};
    public static int[] CHI48 = new int[]{16,17,18};
    
    public static int[] CHI49 = new int[]{19,20,21};
    public static int[] CHI50 = new int[]{20,21,22};
    public static int[] CHI51 = new int[]{21,22,23};
    public static int[] CHI52 = new int[]{22,23,24};
    public static int[] CHI53 = new int[]{23,24,25};
    public static int[] CHI54 = new int[]{24,25,26};
    public static int[] CHI55 = new int[]{25,26,27};

    public static int[] CHI56 = new int[]{32,33,34};
    public static Map<Integer,int[]> chiMap = new ConcurrentHashMap<Integer, int[]>();
    static{
    	chiMap.put(35, CHI35);
    	chiMap.put(36, CHI36);
    	chiMap.put(37, CHI37);
    	chiMap.put(38, CHI38);
    	chiMap.put(39, CHI39);
    	chiMap.put(40, CHI40);
    	chiMap.put(41, CHI41);
    	chiMap.put(42, CHI42);
    	chiMap.put(43, CHI43);
    	chiMap.put(44, CHI44);
    	chiMap.put(45, CHI45);
    	chiMap.put(46, CHI46);
    	chiMap.put(47, CHI47);
    	chiMap.put(48, CHI48);
    	chiMap.put(49, CHI49);
    	chiMap.put(50, CHI50);
    	chiMap.put(51, CHI51);
    	chiMap.put(52, CHI52);
    	chiMap.put(53, CHI53);
    	chiMap.put(54, CHI54);
    	chiMap.put(55, CHI55);
    	chiMap.put(56, CHI56);
    }
    
    /**
     * 番数对分数
     */
    public static Map<Integer,Integer> fanDuiFen = new ConcurrentHashMap<Integer, Integer>();
    static{
    	fanDuiFen.put(1, 1);
    	fanDuiFen.put(2, 2);
    	fanDuiFen.put(3, 5);
    	fanDuiFen.put(4, 10);
    	fanDuiFen.put(5, 20);
    	fanDuiFen.put(6, 50);
    	fanDuiFen.put(7, 100);
    	fanDuiFen.put(8, 200);
    	fanDuiFen.put(9, 500);
    	fanDuiFen.put(10, 1000);
    	fanDuiFen.put(11, 2000);
    	fanDuiFen.put(12, 5000);
    	fanDuiFen.put(13, 10000);
    	fanDuiFen.put(14, 20000);
    	fanDuiFen.put(15, 50000);
    }
    
    public static final int XIHU = 1;
    public static final int GANHU = 2;
    public static final int GANLIGAN = 3;
    public static final int SANGAN = 4;
    public static final int SIGAN = 5;
    public static final int WUGAN = 6;
    public static final int QINGYISE = 7;
    public static final int PENGPENGHU = 8;
    public static final int ZHUANGJIA = 9;
    public static final int ZIMO = 10;    
    public static final int DIANPAO = 11;
    public static final int SANJIABI = 12;

    
    public static final int YAOJIUPENGPENGHU = 99;
    public static final int BUSHIYAOJIUPENGPENGHU = 98;
	public static final ArrayList<Integer> ACTION_YI_JIU =new ArrayList<Integer>();
    static{
    	//吃的1-9
    	ACTION_YI_JIU.add(35);
    	ACTION_YI_JIU.add(41);
    	ACTION_YI_JIU.add(42);
    	ACTION_YI_JIU.add(48);
    	ACTION_YI_JIU.add(49);
    	ACTION_YI_JIU.add(55);
    	//碰的1-9
    	ACTION_YI_JIU.add(57);
    	ACTION_YI_JIU.add(65);
    	ACTION_YI_JIU.add(66);
    	ACTION_YI_JIU.add(74);
    	ACTION_YI_JIU.add(75);
    	ACTION_YI_JIU.add(83);
    	//杠的1-9
    	ACTION_YI_JIU.add(91);
    	ACTION_YI_JIU.add(99);
    	ACTION_YI_JIU.add(100);
    	ACTION_YI_JIU.add(108);
    	ACTION_YI_JIU.add(109);
    	ACTION_YI_JIU.add(117);
    	
    	//碰的中
    	ACTION_YI_JIU.add(88);
    	//杠的中
    	ACTION_YI_JIU.add(122);
    }
    public static final ArrayList<Integer> PAI_YI_JIU =new ArrayList<Integer>();
    static{
    	PAI_YI_JIU.add(1);
    	PAI_YI_JIU.add(9);
    	PAI_YI_JIU.add(10);
    	PAI_YI_JIU.add(18);
    	PAI_YI_JIU.add(19);
    	PAI_YI_JIU.add(27);
    }
}
