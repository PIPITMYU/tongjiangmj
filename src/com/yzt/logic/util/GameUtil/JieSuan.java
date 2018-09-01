package com.yzt.logic.util.GameUtil;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.yzt.logic.mj.domain.Action;
import com.yzt.logic.mj.domain.Player;
import com.yzt.logic.mj.domain.RoomResp;
import com.yzt.logic.util.BackFileUtil;
import com.yzt.logic.util.Cnst;
import com.yzt.logic.util.MahjongUtils;
import com.yzt.logic.util.RoomUtil;
import com.yzt.logic.util.redis.RedisUtil;

/**
 * 玩家分的统计
 * 
 * @author wsw_007
 *
 */
public class JieSuan {
	public static void xiaoJieSuan(String roomId) {
		RoomResp room = RedisUtil.getRoomRespByRoomId(roomId);
		List<Player> players = RedisUtil.getPlayerList(room);
		//需要做以下统计
		//以及大结算校验  这里会写小结算文件 并对房间进行初始化 
		
		//FIXME
		//统计玩家各项数据 庄次数 胡的次数 特殊胡的次数 自摸次数 点炮次数 胡牌类型 具体番数 各个分数统计 
		if(room.getHuangZhuang() != null && room.getHuangZhuang() == true){
			System.out.println("荒庄!");
		}else{
			Integer fen = 0;
			Integer fan = MahjongUtils.checkHuInfo(players,room); // 检查胡牌分数. 每人要给的数
			// 计分方式：1,点炮三家付；
			if (room.getScoreType() == 1) {
				if(room.getWinPlayerId().equals(room.getZhuangId())){ //赢家是庄
					fan = fan +1;
					fen = Cnst.fanDuiFen.get(fan);
					boolean ziMo = false;
					for(Player p: players){
						if(p.getIsHu() && p.getIsZiMo()){
							ziMo = true;
							break;
						}
					}
					if(ziMo){ //赢家是庄 点炮三家付 自摸
						for(Player p: players){
							if(p.getIsHu()){ //自摸的番数已经加过了
								p.setThisScore(fen * 3);
								p.setScore(p.getThisScore() + p.getScore());
							}else{
								if(p.getIsDian()){
									p.setDianNum(p.getDianNum() + 1); //点炮次数加1
								}
								p.setThisScore( - fen);
								p.setScore(p.getThisScore() + p.getScore());
							}
						}
					}else{//赢家是庄 点炮三家付 不是自摸 
						for(Player p: players){
							if(p.getIsHu()){ 
								p.setThisScore(fen * 2 + Cnst.fanDuiFen.get(fan+1));
								p.setScore(p.getThisScore() + p.getScore());
							}else{
								if(p.getIsDian()){
									p.setDianNum(p.getDianNum() + 1); //点炮次数加1
									p.setThisScore(-Cnst.fanDuiFen.get(fan+1));
									p.setScore(p.getThisScore() + p.getScore());
								}else{//其他闲家的
									p.setThisScore( - fen);
									p.setScore(p.getThisScore() + p.getScore());
								}
							}
						}
					}
				}else{ //赢家不是庄，点炮三家付。
					boolean ziMo = false;
					for(Player p: players){
						if(p.getIsHu() && p.getIsZiMo()){
							ziMo = true;
							break;
						}
					}
					fen = Cnst.fanDuiFen.get(fan);
					if(ziMo){ //赢家不是庄，点炮三家付,是自摸
						for(Player p: players){
							if(p.getIsHu()){
								p.setThisScore(fen * 2 + Cnst.fanDuiFen.get(fan+1));
								p.setScore(p.getThisScore() + p.getScore());
							}else{
								if(p.getUserId().equals(room.getZhuangId())){ //输家庄,翻倍
									p.setThisScore( - Cnst.fanDuiFen.get(fan+1));
									p.setScore(p.getThisScore() + p.getScore());
								}else{
									p.setThisScore( - fen);
									p.setScore(p.getThisScore() + p.getScore());
								}
							}
						}
					}else{ ////赢家不是庄,点炮三家付,不是自摸
						boolean dianZhuang = false;
						for(Player p: players){
							if(p.getUserId().equals(room.getZhuangId()) && p.getIsDian()){
								dianZhuang = true;
								break;
							}
						}
						if(dianZhuang){ //点炮的人是庄家
							for(Player p: players){
								if(p.getIsHu()){
									p.setThisScore(fen * 2 + Cnst.fanDuiFen.get(fan+2));
									p.setScore(p.getThisScore() + p.getScore());
								}else{
									if(p.getIsDian()){
										p.setDianNum(p.getDianNum() + 1); //点炮次数加1
										p.setThisScore( - Cnst.fanDuiFen.get(fan+2));
										p.setScore(p.getThisScore() + p.getScore());
									}else{
										p.setThisScore( - fen);
										p.setScore(p.getThisScore() + p.getScore());
									}								
								}
							}
						}else{ //点炮的人不是庄家
							for(Player p: players){
								if(p.getIsHu()){
									p.setThisScore(fen + 2*Cnst.fanDuiFen.get(fan+1));
									p.setScore(p.getThisScore() + p.getScore());
								}else{
									if(p.getIsDian()){
										p.setDianNum(p.getDianNum() + 1); //点炮次数加1
										p.setThisScore( - Cnst.fanDuiFen.get(fan+1));
										p.setScore(p.getThisScore() + p.getScore());
									}else if (p.getUserId().equals(room.getZhuangId())){
										p.setThisScore( - Cnst.fanDuiFen.get(fan+1));
										p.setScore(p.getThisScore() + p.getScore());
									}else{
										p.setThisScore( - fen);
										p.setScore(p.getThisScore() + p.getScore());
									}								
								}
							}
						}
						
					}
				}
			} else {
				//2,赢家为庄 ，点炮包三家
				if(room.getWinPlayerId().equals(room.getZhuangId())){ 
					fan = fan +1;
					fen = Cnst.fanDuiFen.get(fan);
					boolean ziMo = false;
					for(Player p: players){
						if(p.getIsHu() && p.getIsZiMo()){
							ziMo = true;
							break;
						}
					}
					if(ziMo){ //点炮包三家 庄胡 自摸
						for(Player p: players){
							if(p.getIsHu()){
								p.setThisScore(fen * 3 );
								p.setScore(p.getThisScore() + p.getScore());
							}else{
								p.setThisScore( -fen);
								p.setScore(p.getThisScore() + p.getScore());
							}
						}
					}else{ //点炮包三家 庄胡 点炮
						for(Player p: players){
							if(p.getIsHu()){ //自摸的时候已经加过番数
								p.setThisScore(fen * 2 + Cnst.fanDuiFen.get(fan+1));
								p.setScore(p.getThisScore() + p.getScore());
							}
							if(p.getIsDian()){
								p.setDianNum(p.getDianNum() + 1);
								p.setThisScore( -fen * 2 - Cnst.fanDuiFen.get(fan+1));
								p.setScore(p.getThisScore() + p.getScore());
							}
						}
					}
				}else{ //赢家不为庄 点炮包三家
					fen = Cnst.fanDuiFen.get(fan);
					boolean ziMo = false;
					for(Player p: players){
						if(p.getIsHu() && p.getIsZiMo()){
							ziMo = true;
							break;
						}
					}
					if(ziMo){ //赢家不为庄 点炮包三家  胡的玩家是自摸
						for(Player p: players){
							if(p.getIsHu()){
								p.setThisScore(fen * 2 + Cnst.fanDuiFen.get(fan+1));
								p.setScore(p.getThisScore() + p.getScore());
							}else{
								if(p.getUserId().equals(room.getZhuangId())){ //输的庄家翻倍
									p.setThisScore(-Cnst.fanDuiFen.get(fan+1));
									p.setScore(p.getThisScore() + p.getScore());
								}else{
									p.setThisScore(-fen);//闲家正常减分
									p.setScore(p.getThisScore() + p.getScore());
								}
							}
						}
					}else {//赢家不为庄 点炮包三家  胡的玩家是点炮
						boolean dianZhuang = false;
						for(Player p: players){
							if(p.getUserId().equals(room.getZhuangId()) && p.getIsDian()){
								dianZhuang = true;
								break;
							}
						}
						if(dianZhuang){//赢家不为庄 点炮包三家  胡的玩家是点炮   点炮的是庄家
							for(Player p: players){
								if(p.getIsHu()){
									p.setThisScore(fen * 2 + Cnst.fanDuiFen.get(fan+2));
									p.setScore(p.getThisScore() + p.getScore());
								}
								if(p.getIsDian()){
									p.setDianNum(p.getDianNum() + 1); //点炮次数加1
									p.setThisScore(-fen * 2 - Cnst.fanDuiFen.get(fan+2));
									p.setScore(p.getThisScore() + p.getScore());
								}
							}
						}else{//赢家不为庄 点炮包三家  胡的玩家是点炮   点炮的不是庄家
							for(Player p: players){
								if(p.getIsHu()){
									p.setThisScore(fen + 2*Cnst.fanDuiFen.get(fan+1));
									p.setScore(p.getThisScore() + p.getScore());
								}
								if(p.getIsDian()){
									p.setDianNum(p.getDianNum() + 1); //点炮次数加1
									p.setThisScore(-fen - 2*Cnst.fanDuiFen.get(fan+1));
									p.setScore(p.getThisScore() + p.getScore());
								}
							}
						}
						
					}
				}
			}
			
			if(room.getWinPlayerId().equals(room.getZhuangId())){
				//庄不变
			}else{
				//下个人坐庄
				int index = -1;
				Long[] playIds = room.getPlayerIds();
				for(int i=0;i<playIds.length;i++){
					if(playIds[i].equals(room.getZhuangId())){
						index = i+1;
						if(index == 4){
							index = 0;
						}
						break;
					}
				}
				room.setZhuangId(playIds[index]);
				room.setCircleWind(index+1);
				
				//不是第一局,并且圈风是东风 ,证明是下一圈了.
				if(room.getXiaoJuNum() != 1 && room.getCircleWind() == Cnst.WIND_EAST){
					room.setTotolCircleNum(room.getTotolCircleNum() == null ? 1:room.getTotolCircleNum()+1);
					room.setLastNum(room.getCircleNum() - room.getTotolCircleNum());
				}
			}
		}
		
	
		// 更新redis
		RedisUtil.setPlayersList(players);
		
		
		// 添加小结算信息
		List<Integer> xiaoJS = new ArrayList<Integer>();
		for (Player p : players) {
			xiaoJS.add(p.getThisScore());
		}
		room.addXiaoJuInfo(xiaoJS);
		// 初始化房间
		room.initRoom();
		RedisUtil.updateRedisData(room, null);
		// 写入文件
		List<Map<String, Object>> userInfos = new ArrayList<Map<String, Object>>();
		for (Player p : players) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("userId", p.getUserId());
			map.put("score", p.getThisScore());
			map.put("pais", p.getCurrentMjList());
			map.put("duiNum", p.getDuiHunNum());
			if(p.getIsHu()){
				map.put("isWin", 1);
				map.put("winInfo", p.getFanShu());
			}else{
				map.put("isWin", 0);
			}
			if(p.getIsDian()){
				map.put("isDian", 1);
			}else{
				map.put("isDian", 0);
			}
			if(p.getActionList() != null && p.getActionList().size() > 0){
				List<Object> actionList = new ArrayList<Object>();
				for(Action action : p.getActionList()){
					if(action.getType() == Cnst.ACTION_TYPE_CHI){
						Map<String,Integer> actionMap = new HashMap<String, Integer>();
						actionMap.put("action", action.getActionId());
						actionMap.put("extra", action.getExtra());
						actionList.add(actionMap);
						
					}else if(action.getType() == Cnst.ACTION_TYPE_ANGANG){
						Map<String,Integer> actionMap = new HashMap<String, Integer>();
						actionMap.put("action", action.getActionId());
						actionMap.put("extra", action.getExtra());
						actionList.add(actionMap);
					}else{
						actionList.add(action.getActionId());
					}
				}
				map.put("actionList", actionList);
			}			
			userInfos.add(map);
		}
		JSONObject info = new JSONObject();
		info.put("lastNum", room.getLastNum());
		info.put("userInfo", userInfos);
		BackFileUtil.save(100102, room, null, info,null);
		// 小结算 存入一次回放
		BackFileUtil.write(room);

		// 大结算判定 (玩的圈数等于选择的圈数)
		if (room.getTotolCircleNum() == room.getCircleNum()) {
			// 最后一局 大结算
			room = RedisUtil.getRoomRespByRoomId(roomId);
			room.setState(Cnst.ROOM_STATE_YJS);
			RedisUtil.updateRedisData(room, null);
			// 这里更新数据库吧
			RoomUtil.updateDatabasePlayRecord(room);
		}
	}

}
