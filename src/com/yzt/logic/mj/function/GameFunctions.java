package com.yzt.logic.mj.function;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.yzt.logic.mj.domain.Action;
import com.yzt.logic.mj.domain.ClubInfo;
import com.yzt.logic.mj.domain.DissolveRoom;
import com.yzt.logic.mj.domain.Player;
import com.yzt.logic.mj.domain.RoomResp;
import com.yzt.logic.util.BackFileUtil;
import com.yzt.logic.util.Cnst;
import com.yzt.logic.util.MahjongUtils;
import com.yzt.logic.util.RoomUtil;
import com.yzt.logic.util.GameUtil.JieSuan;
import com.yzt.logic.util.GameUtil.StringUtils;
import com.yzt.logic.util.redis.RedisUtil;
import com.yzt.netty.client.WSClient;
import com.yzt.netty.util.MessageUtils;


/**
 * Created by Administrator on 2017/7/13. 游戏中
 */

public class GameFunctions extends TCPGameFunctions {
	final static Object object = new Object();
	/**
	 * 用户点击准备，用在小结算那里，
	 * 
	 * @param session
	 * @param readData
	 */
	public synchronized static void interface_100200(WSClient channel,Map<String, Object> readData) {
		logger.info("准备,interfaceId -> 100200");

		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Long userId = StringUtils.parseLong(readData.get("userId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));

		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId));
		Player currentPlayer = null;
		List<Player> players = RedisUtil.getPlayerList(room);
		for (Player p : players) {
			if (p.getUserId().equals(userId)) {
				currentPlayer = p;
				break;
			}
		}

		if (room.getState() == Cnst.ROOM_STATE_GAMIING) {
			return;
		}
//		if (currentPlayer == null
//				|| currentPlayer.getPlayStatus() == Cnst.PLAYER_STATE_PREPARED) {
//			return;
//		}
		//TODO 
		currentPlayer.initPlayer(currentPlayer.getRoomId(),Cnst.PLAYER_STATE_PREPARED,currentPlayer.getScore());

		boolean allPrepared = true;

		for (Player p : players) {
			if (!p.getPlayStatus().equals(Cnst.PLAYER_STATE_PREPARED)) {
				allPrepared = false;
			}
		}

		if (allPrepared&&players!=null&&players.size()==4) {
			if (room.getRoomType() == Cnst.ROOM_TYPE_2 ) {
				MessageFunctions.interface_100112(null, room, Cnst.PLAYER_EXTRATYPE_KAIJU);
			}
			startGame(room, players);		        
			BackFileUtil.save(interfaceId, room,players,null,null);//写入文件内容					 
		}
		Map<String, Object> info = new HashMap<String, Object>();
		List<Map<String, Object>> userInfo = new ArrayList<Map<String, Object>>();
		for (Player p : players) {
			Map<String, Object> i = new HashMap<String, Object>();
			i.put("userId", p.getUserId());
			i.put("playStatus", p.getPlayStatus());			
			userInfo.add(i);
		}
		info.put("userInfo", userInfo);
		Map<String, Object> roominfo = new HashMap<String, Object>();
		roominfo.put("state", room.getState());
		info.put("roomInfo", roominfo);
		for (Player p : players) {
			WSClient ws = getWSClientManager().getWSClient(p.getChannelId());
			if(ws == null)
				continue;
			if(room.getState() == Cnst.ROOM_STATE_GAMIING ){
				info.put("extra", p.getCurrentMjList()); 				
				info.put("nextActionUserId",room.getNextActionUserId());
				info.put("nextAction", room.getNextAction());
				info.put("hunPai", room.getHunPai());
				info.put("dingHunPai", room.getDingHunPai());
			}		
			JSONObject result = getJSONObj(interfaceId, 1, info);
			MessageUtils.sendMessage(ws, result.toJSONString());
		}
		RedisUtil.setPlayersList(players);
		RedisUtil.updateRedisData(room, null);
	}

	/**
	 * 开局发牌
	 * 
	 * @param roomId
	 */
	public static void startGame(RoomResp room, List<Player> players) {
		//局数统计
		room.setXiaoJuNum(room.getXiaoJuNum() == null ? 1 : room.getXiaoJuNum() + 1);
		room.setXjst(System.currentTimeMillis());
		room.setState(Cnst.ROOM_STATE_GAMIING);
		
		//获得所需要的牌型
		room.setCurrentMjList(MahjongUtils.getPais()); 
		
		//发牌
		Player zhuangPlayer = null;
		for(Player p : players) {
			p.setPlayStatus(Cnst.PLAYER_STATE_GAME);//游戏中..

			if(room.getZhuangId().equals(p.getUserId())) {

				zhuangPlayer = p;
				p.setZhuangNum(p.getZhuangNum() == null ? 1	: p.getZhuangNum() + 1);//坐庄的次数
				p.setCurrentMjList(MahjongUtils.paiXu(MahjongUtils.faPai(room.getCurrentMjList(), 14)));
//				p.setCurrentMjList(MahjongUtils.paiXu(MahjongUtils.faPai(14)));
			}else{
				p.setCurrentMjList(MahjongUtils.paiXu(MahjongUtils.faPai(room.getCurrentMjList(), 13)));
//				p.setCurrentMjList(MahjongUtils.paiXu(MahjongUtils.faPai(13)));
			}
			
		}
		//定混牌 (不包含东南西北白发) 把混牌从牌墩中去除.
		Integer hunPai = MahjongUtils.dingHunPai(room.getCurrentMjList(),room);
		room.setHunPai(hunPai);
		//3.1看庄家有没有暗杠.带混检测, 有没有胡牌等.
		List<Integer> actionList = MahjongUtils.checkActionList(zhuangPlayer,zhuangPlayer.getCurrentMjList().get(13),room,Cnst.CHECK_TYPE_ZIJIMO,false);
		room.setNextAction(actionList);//动作集合发给前端的.
		room.setNextActionUserId(room.getZhuangId());
		//设置最后发牌人
		room.setLastFaPai(zhuangPlayer.getCurrentMjList().get(13));
		room.setLastFaPaiUserId(room.getZhuangId());
		if(room.getXiaoJuNum() == 1){
			//获取此俱乐部当天活跃总人数
			if(String.valueOf(room.getRoomId()).length()==7){//说明是俱乐部
				//今日活跃数  key: cid  value ：userId的集合
				Long timesmorning = StringUtils.getTimesmorning();
				Long scard = RedisUtil.scard(room.getClubId()+"_".concat(timesmorning+""));
				int dieTime=Cnst.REDIS_CLUB_DIE_TIME;
				if(scard==null || scard==0l){//当天没人,有人最少为5
					//创建一个并设置过期时间(其中1l为假数据)--昨日和前日
					//假数据主要是为了设置过期时间--此时间只设置一次
					RedisUtil.sadd(room.getClubId()+"_".concat(timesmorning+""),1l,dieTime);
					for (Long userId : room.getPlayerIds()) {
						RedisUtil.sadd(room.getClubId()+"_".concat(timesmorning+""),userId,null);
					}
				}else{//有人
					for (Long userId : room.getPlayerIds()) {
						RedisUtil.sadd(room.getClubId()+"_".concat(timesmorning+""),userId,null);
					}
				}
				//今日俱乐部局数   --昨日和前日
				Integer clubId = room.getClubId();
				Integer todayJuNum = RedisUtil.getTodayJuNum(clubId+"".concat(timesmorning+""));
				if(todayJuNum==null || todayJuNum==0){
					RedisUtil.setTodayJuNum(clubId+"".concat(timesmorning+""),1,dieTime);
				}else{
					RedisUtil.setTodayJuNum(clubId+"".concat(timesmorning+""),1+todayJuNum,null);
				}
				Long[] playerIds = room.getPlayerIds();
				//今日玩家局数  --保存一天
				Integer juNum=null;
				for (Long playerId : playerIds) {
					//key clubId+userId+今天早上时间
					juNum = RedisUtil.getObject(Cnst.REDIS_CLUB_TODAYJUNUM_ROE_USER.concat(clubId+"_").concat(playerId+"").concat(timesmorning+""), Integer.class);
					if(juNum==null || juNum==0){
						RedisUtil.setObject(Cnst.REDIS_CLUB_TODAYJUNUM_ROE_USER.concat(clubId+"_").concat(playerId+"").concat(timesmorning+""),1, Cnst.REDIS_CLUB_PLAYERJUNUM_TIME);
					}else{
						RedisUtil.setObject(Cnst.REDIS_CLUB_TODAYJUNUM_ROE_USER.concat(clubId+"_").concat(playerId+"").concat(timesmorning+""),juNum+1,Cnst.REDIS_CLUB_PLAYERJUNUM_TIME );
					}
				}
				RedisUtil.hdel(Cnst.REDIS_CLUB_ROOM_LIST.concat(String.valueOf(room.getClubId())), String.valueOf(room.getRoomId()));
			}
			
			//TODO 关闭定时解散房间任务  
			RoomUtil.addRoomToDB(room);
			RoomUtil.removeFreeRoomTask(StringUtils.parseLong(room.getRoomId()));
		}
	}
	public static void main(String[] args) {
		long currentTimeMillis = System.currentTimeMillis();
		System.out.println(currentTimeMillis);
		Date date =new Date(0);
		SimpleDateFormat sm=new SimpleDateFormat("YY-mm-dd");
		sm.format(date);
		System.out.println(sm);
		System.out.println(date);
	}
	/**
	 * 出牌
	 * 行为编码(游戏内主逻辑)
	 * @param wsClient
	 * @param readData
	 */
	public synchronized static void interface_100201(WSClient channel, Map<String, Object> readData) {
		logger.info("游戏内主逻辑,interfaceId -> 100201");
		
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
	    Integer action = StringUtils.parseInt(readData.get("action")); //行为编号,牌的信息
		if(action == -4){
			return;
		}
	    Integer roomSn = StringUtils.parseInt(readData.get("roomSn")); //房间号
		Long userId = StringUtils.parseLong(readData.get("userId")); //玩家ID
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomSn));
		if(!room.getNextActionUserId().equals(userId)){
			return;
		}		
		List<Player> players = RedisUtil.getPlayerList(room);
		
		Player currentPlayer = null;
		for (Player p : players) {
			if (p.getUserId().equals(userId)) {
				currentPlayer = p;
				break;
			}
		}
		if (currentPlayer == null) {
			return;
		}
		//设置上个吃碰杠动作统计
		if(action != null && action >= 1 && action <=126){
			room.setLastAction(action);
			room.setLastActionUserId(userId);
			if(action >=35 && action <=126){
				//清空 过 集合
				room.getGuoUserIds().clear();
			}
			
		}
		//不提示怼
		if(action == room.getHunPai()){
			action = -3;
		}
		List<Integer> currentMjList = currentPlayer.getCurrentMjList();
		Action ac = null;
		//判断动作
		in:if(action == -1){//发牌
			logger.info("-1 请求发牌!");
			List<Integer> faPai = MahjongUtils.faPai(room.getCurrentMjList(), 1);
			if(faPai == null){//荒庄
				ac = new Action(Cnst.ACTION_TYPE_FAPAI,action,userId,null,null );
				room.setState(Cnst.ROOM_STATE_XJS);
				room.setHuangZhuang(true);
				break in;
			}
			room.getGuoUserIds().clear();
			//海底牌玩家不用出牌
			if(room.getCurrentMjList().size() < 12){ 
				room.getGuoUserIds().clear();		
				Integer haiDiPai = faPai.get(0);
				room.setLastFaPai(haiDiPai); 
				room.setLastFaPaiUserId(userId);
				currentMjList.add(haiDiPai);
				List<Integer> currentPlayerActionList = MahjongUtils.checkActionList(currentPlayer, haiDiPai, room,Cnst.CHECK_TYPE_HAIDIANPAI,false);
				ac = new Action(Cnst.ACTION_TYPE_FAPAI,action,userId,null,haiDiPai);
				if(currentPlayerActionList != null && currentPlayerActionList.size() > 0){
					room.setNextAction(currentPlayerActionList);
					room.setNextActionUserId(userId);
				}else{
					getNextFaPaiHaiDi(room,userId);
				}
			}else{
				Integer pai = faPai.get(0);
				//添加玩家手牌
				currentMjList.add(pai);
				ac = new Action(Cnst.ACTION_TYPE_FAPAI,action,userId,null,pai);
				List<Integer> currentPlayerActionList = MahjongUtils.checkActionList(currentPlayer, pai, room,Cnst.CHECK_TYPE_ZIJIMO,false);
				//设置最后发牌人
				room.setLastFaPai(pai); 
				room.setLastFaPaiUserId(userId);
				room.setNextAction(currentPlayerActionList);
				room.setNextActionUserId(userId);		
			}		
		//过	
		}else if(action == 0){
			logger.info("过!!!!!!");
			ac = new Action(Cnst.ACTION_TYPE_GUO,action,userId,null,null);
//			currentPlayer.getFanShu().clear();
			if(room.getLastFaPaiUserId().equals(userId) && room.getGuoUserIds().size() == 0){
				//自己摸到牌点过 
				List<Integer> nextAction = new ArrayList<Integer>();
				nextAction.add(501);
				room.setNextAction(nextAction);
				room.setNextActionUserId(userId);
			}else{
				//点击过的人的集合
				room.getGuoUserIds().add(currentPlayer.getUserId());
				//最后一个人点击过,数量为4.这个牌没人需要.开始发下一张牌. 并清空点过的集合
				if(room.getGuoUserIds().size() == 4){
					room.getGuoUserIds().clear();

					getNextFaPai(room);
				}else{
					MahjongUtils.getNextAction(players,room,room.getLastChuPai());
					if(room.getNextAction() != null && room.getNextAction().contains(501)){
						logger.error("fuckkkkkkkkkkkkkkkkkkkkkkkkkkkk=====userId"+room.getNextActionUserId()+"action"+room.getNextAction()+"guoList"+room.getGuoUserIds());
					}
				}
			}	
		//胡	
		}else if(action == 500){
			logger.info("当前用户选择胡了!!!");
			
			room.setWinPlayerId(userId);
			currentPlayer.setIsHu(true);
			//先保留胡的最后一张牌
			if(room.getLastFaPaiUserId().equals(userId) && room.getGuoUserIds().size() == 0){
				//自摸 
				ac = new Action(Cnst.ACTION_TYPE_HU,action,userId,userId,null);
				currentPlayer.setHuDePai(room.getLastFaPai());
				currentPlayer.setIsZiMo(true);
				
			}else{
				ac = new Action(Cnst.ACTION_TYPE_HU,action,userId,room.getLastChuPaiUserId(),null);
				currentPlayer.setHuDePai(room.getLastChuPai());
				currentMjList.add(room.getLastChuPai());
				for(Player p:players){
					if(p.getUserId().equals(room.getLastChuPaiUserId())){
						p.setIsDian(true);
						RedisUtil.updateRedisData(null, p);
						break;
					}
				}
			}
			//进入小结算 统计玩家数据
			room.setState(Cnst.ROOM_STATE_XJS);
			
		//怼牌
		}else if(action == -3){
			logger.info("-3    当前用户选择怼混了!!!");
			ac = new Action(Cnst.ACTION_TYPE_DUIHUN,action,userId,null,room.getHunPai());
			currentPlayer.setDuiHunNum(currentPlayer.getDuiHunNum()+1);//怼牌次数+1
			MahjongUtils.removePai(currentPlayer, room.getHunPai());
//			currentPlayer.getChuList().add(room.getHunPai());
			List<Integer> nextAction = new ArrayList<Integer>();
			nextAction.add(-1);
			room.setNextAction(nextAction);
			//取到上个出牌人的角标 下一位来发牌
			int index = -1;
			Long[] playIds = room.getPlayerIds();
			for(int i=0;i<playIds.length;i++){
				if(playIds[i].equals(userId)){
					index = i+1;
					if(index == 4){
						index = 0;
					}
					break;
				}
			}
			room.setNextActionUserId(playIds[index]);
			
		//出牌(移除玩家手中的牌)
		}else if(action <= 34 && action >= 1){ 
			logger.info("1-34中的牌    出牌!!!");
			ac = new Action(Cnst.ACTION_TYPE_CHUPAI,action,userId,null,null);
			//设置最后出牌的玩家
			room.setLastChuPaiUserId(userId);
			room.setLastChuPai(action);
			//移除手牌 添加已出过的牌
			MahjongUtils.removePai(currentPlayer, action);
			currentPlayer.getChuList().add(action);
			//遍历所有玩家的动作集合.设定  过list
			room.getGuoUserIds().add(currentPlayer.getUserId());
			//检测所有玩家的动作并排序.
			MahjongUtils.getNextAction(players,room,action);
			
		//吃	
		}else if(action >=35 && action <=56){ 
			logger.info("35-56中的牌    我吃了!!!");
			
			List<Integer> chi = MahjongUtils.reChiList(action, room.getLastChuPai());//吃完组成的集合
			//玩家 动作集合
			ac = new Action(Cnst.ACTION_TYPE_CHI,action,currentPlayer.getUserId(),room.getLastChuPaiUserId(),room.getLastChuPai());
			currentPlayer.addActionList(ac);
			//移除手牌
			MahjongUtils.removeActionMj(currentMjList, chi, action, Cnst.ACTION_TYPE_CHI);
			//移除桌面上被玩家吃掉的牌
			MahjongUtils.removeCPG(room, players);
			//吃完是请求出牌
			List<Integer> actionList = new ArrayList<Integer>();
			actionList.add(501);
			room.setNextAction(actionList);
			room.setNextActionUserId(userId);

		//碰
		}else if(action >=57 && action <=90){
			logger.info("57-90中的牌    我碰了啊!!!");
			//玩家 动作集合
			//Integer type, Integer actionId, Long userId, Long toUserId, Integer extra
			ac = new Action(Cnst.ACTION_TYPE_PENG,action,currentPlayer.getUserId(),room.getLastChuPaiUserId(),room.getLastChuPai());
			currentPlayer.addActionList(ac);
			//移除手牌
			MahjongUtils.removeActionMj(currentMjList, null, action, Cnst.ACTION_TYPE_PENG);
			//移除桌面上被玩家吃掉的牌
			MahjongUtils.removeCPG(room, players);
			
			//碰完是请求出牌
			List<Integer> actionList = new ArrayList<Integer>();
			actionList.add(501);
			room.setNextAction(actionList);
			room.setNextActionUserId(userId);
	
		//杠	
		}else if(action >=91 && action <=126){
			logger.info("91-126中的牌    我杠了啊!!!");
			Integer pai = action - 90;//杠的牌
			//这里需要检测是碰杠 点杠 或 暗杠
			Iterator<Integer> it = currentMjList.iterator();
			Integer gangPaiNum = 0;
			while(it.hasNext()){
				Integer x = it.next();
			    if(x==pai){
			    	gangPaiNum ++ ;
			    }
			}
			if(gangPaiNum == 1){
				//碰杠  先遍历用户动作集合 移除碰的集合 加上碰杠操作
				
				List<Action> actionList = currentPlayer.getActionList();
				Long toUserId = null;
				for(Action a:actionList){
					if(a.getType() == Cnst.ACTION_TYPE_PENG && a.getExtra() == pai){
						toUserId = a.getToUserId();
						actionList.remove(a);
						break;
					}
				}
				ac = new Action(Cnst.ACTION_TYPE_PENGGANG,action,currentPlayer.getUserId(),toUserId,pai);
				actionList.add(ac);
				currentPlayer.setActionList(actionList);
				MahjongUtils.removeActionMj(currentMjList, null, action, Cnst.ACTION_TYPE_PENGGANG);
				//移除桌面上被玩家吃掉的牌
				MahjongUtils.removeCPG(room, players);
			}else if(gangPaiNum == 4){
				//暗杠 		
				ac = new Action(Cnst.ACTION_TYPE_ANGANG,action,userId,null,pai);
				currentPlayer.addActionList(ac);
				//移除手牌
				MahjongUtils.removeActionMj(currentMjList, null, action, Cnst.ACTION_TYPE_ANGANG);
				//移除桌面上被玩家吃掉的牌
				MahjongUtils.removeCPG(room, players);
			}else if(gangPaiNum == 3){
				//点杠
				ac = new Action(Cnst.ACTION_TYPE_DIANGANG,action,userId,room.getLastChuPaiUserId(),pai);
				currentPlayer.addActionList(ac);
				//移除手牌
				MahjongUtils.removeActionMj(currentMjList, null, action, Cnst.ACTION_TYPE_DIANGANG);
				//移除桌面上被玩家吃掉的牌
				MahjongUtils.removeCPG(room, players);
			}
			//杠完摸牌
			List<Integer> nextAction = new ArrayList<Integer>();
			nextAction.add(-1);
			room.setNextAction(nextAction);
			room.setNextActionUserId(userId);
			
		}

		// TODO 更新Redis
				RedisUtil.updateRedisData(room, currentPlayer);
//				RedisUtil.setPlayersList(players);
				//写入回放
				BackFileUtil.save(interfaceId, room, players, null, ac);
				//小结算
				if(room.getState() == Cnst.ROOM_STATE_XJS){
					JieSuan.xiaoJieSuan(String.valueOf(roomSn));
				}
				//统一发消息即可 跟据action 来判断
				MessageFunctions.interface_100104(room, players, 100104,ac);
	}


	/**
	 * 通过下标获取到下一个要发牌的人. 正常 发牌
	 * @param room
	 */
	private static void getNextFaPaiHaiDi(RoomResp room,Long userId) {
		List<Integer> nextAction = new ArrayList<Integer>();
		nextAction.add(-1);
		room.setNextAction(nextAction);
		//取到上个出牌人的角标 下一位来发牌
		int index = -1;
		Long[] playIds = room.getPlayerIds();
		for(int i=0;i<playIds.length;i++){
			if(playIds[i].equals(userId)){
				index = i+1;
				if(index == 4){
					index = 0;
				}
				break;
			}
		}
		room.setNextActionUserId(playIds[index]);
	}
	
	/**
	 * 通过下标获取到下一个要发牌的人. 海底牌
	 * @param room
	 */
	private static void getNextFaPai(RoomResp room) {
		List<Integer> nextAction = new ArrayList<Integer>();
		nextAction.add(-1);
		room.setNextAction(nextAction);
		//取到上个出牌人的角标 下一位来发牌
		int index = -1;
		Long[] playIds = room.getPlayerIds();
		for(int i=0;i<playIds.length;i++){
			if(playIds[i].equals(room.getLastChuPaiUserId())){
				index = i+1;
				if(index == 4){
					index = 0;
				}
				break;
			}
		}
		room.setNextActionUserId(playIds[index]);
	}

	/**
	 * 玩家申请解散房间
	 * 
	 * @param session
	 * @param readData
	 * @throws Exception
	 */
	public synchronized static void interface_100203(WSClient channel,Map<String, Object> readData) throws Exception {
		logger.info("玩家请求解散房间,interfaceId -> 100203");
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));
		Long userId = StringUtils.parseLong(readData.get("userId"));
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId));
		if (room.getDissolveRoom() != null) {
			return;
		}
		DissolveRoom dis = new DissolveRoom();
		dis.setDissolveTime(new Date().getTime());
		dis.setUserId(userId);
		List<Map<String, Object>> othersAgree = new ArrayList<>();
		List<Player> players = RedisUtil.getPlayerList(room);
		for (Player p : players) {
			if (!p.getUserId().equals(userId)) {
				Map<String, Object> map = new HashMap<>();
				map.put("userId", p.getUserId());
				map.put("agree", 0);// 1同意；2解散；0等待
				othersAgree.add(map);
			}
		}
		dis.setOthersAgree(othersAgree);
		room.setDissolveRoom(dis);

		Map<String, Object> info = new HashMap<>();
		info.put("dissolveTime", dis.getDissolveTime());
		info.put("userId", dis.getUserId());
		info.put("othersAgree", dis.getOthersAgree());
		JSONObject result = getJSONObj(interfaceId, 1, info);
		for (Player p : players) {
			WSClient ws = getWSClientManager().getWSClient(p.getChannelId());
			if (ws != null ) {
				MessageUtils.sendMessage(ws, result.toJSONString());
			}
		}

		for (Player p : players) {
			RedisUtil.updateRedisData(null, p);
		}
		RedisUtil.updateRedisData(room, null);
		
		//解散房间超时任务开启 TODO
		RoomUtil.addFreeRoomTask(StringUtils.parseLong(room.getRoomId()), System.currentTimeMillis()+Cnst.ROOM_DIS_TIME);
	}

	/**
	 * 同意或者拒绝解散房间
	 * 
	 * @param session
	 * @param readData
	 * @throws Exception
	 */

	public synchronized static void interface_100204(WSClient channel,Map<String, Object> readData) throws Exception {
		logger.info("同意或者拒绝解散房间,interfaceId -> interface_100204");
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));
		Long userId = StringUtils.parseLong(readData.get("userId"));
		Integer userAgree = StringUtils.parseInt(readData.get("userAgree"));
		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId));
		if (room == null) {// 房间已经自动解散
			Map<String, Object> info = new HashMap<>();
			info.put("reqState", Cnst.REQ_STATE_4);
			JSONObject result = getJSONObj(interfaceId, 1, info);
			MessageUtils.sendMessage(channel, result.toJSONString());
			return;
		}
		if (room.getDissolveRoom() == null) {
			Map<String, Object> info = new HashMap<>();
			info.put("reqState", Cnst.REQ_STATE_7);
			JSONObject result = getJSONObj(interfaceId, 1, info);
			MessageUtils.sendMessage(channel, result.toJSONString());
			return;
		}
		List<Map<String, Object>> othersAgree = room.getDissolveRoom().getOthersAgree();
		for (Map<String, Object> m : othersAgree) {
			if (String.valueOf(m.get("userId")).equals(String.valueOf(userId))) {
				m.put("agree", userAgree);
				break;
			}
		}
		Map<String, Object> info = new HashMap<>();
		info.put("dissolveTime", room.getDissolveRoom().getDissolveTime());
		info.put("userId", room.getDissolveRoom().getUserId());
		info.put("othersAgree", room.getDissolveRoom().getOthersAgree());
		JSONObject result = getJSONObj(interfaceId, 1, info);

//		if (userAgree == 2) {
//			//有玩家拒绝解散房间//关闭解散房间计时任务 TODO
//			RoomUtil.removeFreeRoomTask(StringUtils.parseLong(room.getRoomId()));
//			room.setDissolveRoom(null);
//			RedisUtil.setObject(Cnst.REDIS_PREFIX_ROOMMAP.concat(String.valueOf(roomId)), room,	Cnst.ROOM_LIFE_TIME_CREAT);
//		}
		int agreeNum = 0;
		int rejectNum = 0;
		for (Map<String, Object> m : othersAgree) {
			if (m.get("agree").equals(1)) {
				agreeNum++;
			} else if (m.get("agree").equals(2)) {
				rejectNum++;
			}
		}
		if(rejectNum >= 2 ){
			//有玩家拒绝解散房间//关闭解散房间计时任务 TODO
			RoomUtil.removeFreeRoomTask(StringUtils.parseLong(room.getRoomId()));
			room.setDissolveRoom(null);
			RedisUtil.setObject(Cnst.REDIS_PREFIX_ROOMMAP.concat(String.valueOf(roomId)), room,	Cnst.ROOM_LIFE_TIME_CREAT);
		}
		RedisUtil.updateRedisData(room, null);
		List<Player> players = RedisUtil.getPlayerList(room);

		if (agreeNum >= 2) {
				//解散房间是 xiaoJSInfo 写入0
				if(room.getState() == Cnst.ROOM_STATE_GAMIING){
					//中途准备阶段解散房间不计入回放中
					List<Integer> xiaoJSInfo = new ArrayList<Integer>();
					for(int i=0;i<room.getPlayerIds().length;i++){
						xiaoJSInfo.add(0);
					}
					room.addXiaoJuInfo(xiaoJSInfo);
				}
				if (room.getRoomType() == Cnst.ROOM_TYPE_2) {
					MessageFunctions.interface_100112(null, room,Cnst.PLAYER_EXTRATYPE_JIESANROOM);
				}
				room.setState(Cnst.ROOM_STATE_YJS);
				
				RoomUtil.updateDatabasePlayRecord(room);
				for (Player p : players) {
					//TODO
					p.initPlayer(null,Cnst.PLAYER_STATE_DATING,null);
				}
				room.setDissolveRoom(null);
				RedisUtil.setObject(Cnst.REDIS_PREFIX_ROOMMAP.concat(String.valueOf(roomId)), room, Cnst.ROOM_LIFE_TIME_DIS);
				RedisUtil.setPlayersList(players);
				//关闭解散房间计时任务 TODO
				RoomUtil.removeFreeRoomTask(StringUtils.parseLong(room.getRoomId()));
		}
		

		for (Player p : players) {
			WSClient ws = getWSClientManager().getWSClient(p.getChannelId());
			if (ws != null) {
				MessageUtils.sendMessage(ws, result.toJSONString());
			}
		}

	}

	/**
	 * 退出房间
	 * 
	 * @param session
	 * @param readData
	 * @throws Exception
	 */
	public synchronized static void interface_100205(WSClient channel,Map<String, Object> readData) throws Exception {
		logger.info("退出房间,interfaceId -> 100205");
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));
		Long userId = StringUtils.parseLong(readData.get("userId"));

		RoomResp room = RedisUtil.getRoomRespByRoomId(String.valueOf(roomId));
		if (room == null) {
			roomDoesNotExist(interfaceId, channel);
			return;
		}
		if (room.getState() == Cnst.ROOM_STATE_CREATED) {
			List<Player> players = RedisUtil.getPlayerList(room);
			Map<String, Object> info = new HashMap<>();
			info.put("userId", userId);
			if (room.getCreateId().equals(userId)) {// 房主退出，
				if (room.getRoomType().equals(Cnst.ROOM_TYPE_1)) {// 房主模式
					int circle = room.getCircleNum();

				
					info.put("type", Cnst.EXIST_TYPE_DISSOLVE);
					if(String.valueOf(roomId).length() == 6){
						for (Player p : players) {
							if (p.getUserId().equals(userId)) {
								p.setMoney(p.getMoney() + Cnst.moneyMap.get(circle));
								break;
							}
						}
					}else if(String.valueOf(roomId).length() == 7){
						//退还俱乐部房卡
						ClubInfo clubInfo = RedisUtil.getClubInfoByClubId(room.getClubId().toString());
						clubInfo.setRoomCardNum(clubInfo.getRoomCardNum()+Cnst.moneyMap.get(room.getCircleNum()));
						RedisUtil.setClubInfoByClubId(String.valueOf(room.getClubId()), clubInfo);
						//移除俱乐部创建房间缓存
						RedisUtil.hdel(Cnst.REDIS_CLUB_ROOM_LIST.concat(String.valueOf(room.getClubId())), String.valueOf(room.getRoomId()));
					}

					RedisUtil.deleteByKey(Cnst.REDIS_PREFIX_ROOMMAP
							.concat(String.valueOf(roomId)));

					for (Player p : players) {
						//TODO 
						p.initPlayer(null,Cnst.PLAYER_STATE_DATING,null);
					}
					 //关闭解散房间计时任务 TODO
					RoomUtil.removeFreeRoomTask(StringUtils.parseLong(room.getRoomId()));
				} else {// 自由模式，走正常退出
					info.put("type", Cnst.EXIST_TYPE_EXIST);
					existRoom(room, players, userId);
					RedisUtil.updateRedisData(room, null);					
				}
			} else {// 正常退出
				for (Player player : players) {
					if(player.getUserId().equals(userId)){//找到退出的玩家
						// 如果加入的代开房间 通知房主
						if (room.getRoomType() == Cnst.ROOM_TYPE_2
								&& !userId.equals(room.getCreateId())) {
							MessageFunctions.interface_100112(player, room,Cnst.PLAYER_EXTRATYPE_EXITROOM);
						}
					}
				}
				info.put("type", Cnst.EXIST_TYPE_EXIST);
				existRoom(room, players, userId);
				RedisUtil.updateRedisData(room, null);							
			}
			JSONObject result = getJSONObj(interfaceId, 1, info);
			for (Player p : players) {
				RedisUtil.updateRedisData(null, p);
			}

			for (Player p : players) {
				WSClient ws = getWSClientManager().getWSClient(p.getChannelId());
				if (ws != null) {
					MessageUtils.sendMessage(ws,result.toJSONString());
				}
			}

		} else {
			roomIsGaming(interfaceId, channel);
		}
	}

	private static void existRoom(RoomResp room, List<Player> players, Long userId) {
		for (Player p : players) {
			if (p.getUserId().equals(userId)) {
				//TODO
				p.initPlayer(null,Cnst.PLAYER_STATE_DATING,null);
				break;
			}
		}
		Long[] pids = room.getPlayerIds();
		if (pids != null) {
			for (int i = 0; i < pids.length; i++) {
				if (userId.equals(pids[i])) {
					pids[i] = null;
					break;
				}
			}
		}
	}

	/**
	 * 语音表情
	 * 
	 * @param session
	 * @param readData
	 * @throws Exception
	 */
	public static void interface_100206(WSClient channel,Map<String, Object> readData) throws Exception {
		logger.info("语音表情,interfaceId -> 100206");
		Integer interfaceId = StringUtils.parseInt(readData.get("interfaceId"));
		Integer roomId = StringUtils.parseInt(readData.get("roomSn"));
		String userId = String.valueOf(readData.get("userId"));
		String type = String.valueOf(readData.get("type"));
		String idx = String.valueOf(readData.get("idx"));

		Map<String, Object> info = new HashMap<>();
		info.put("roomId", roomId);
		info.put("userId", userId);
		info.put("type", type);
		info.put("idx", idx);
		JSONObject result = getJSONObj(interfaceId, 1, info);
		List<Player> players = RedisUtil.getPlayerList(roomId);
		for (Player p : players) {
			if (!p.getUserId().equals(userId)) {
				WSClient ws = getWSClientManager().getWSClient(p.getChannelId());
				if (ws != null) {
					MessageUtils.sendMessage(ws, result.toJSONString());
				}
			}
		}
	}
	

}
