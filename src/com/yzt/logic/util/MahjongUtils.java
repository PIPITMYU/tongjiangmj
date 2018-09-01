package com.yzt.logic.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;







import com.yzt.logic.mj.domain.Action;
import com.yzt.logic.mj.domain.Player;
import com.yzt.logic.mj.domain.RoomResp;
import com.yzt.logic.util.JudegHu.checkHu.Hulib;
import com.yzt.logic.util.JudegHu.checkHu.TableMgr;
import com.yzt.logic.util.redis.RedisUtil;

/**
 * 
 * @author wsw_007
 *
 */
public class MahjongUtils {

	static {
		// 加载胡的可能
		TableMgr.getInstance().load();
	}
	
	
	/**
	 * 获得所需要的牌型(干里干) 并打乱牌型
	 * 
	 * @return
	 */
	public static List<Integer> getPais() {
		// 1-9万 ,10-18饼,19-27条,32红中.
		ArrayList<Integer> pais = new ArrayList<Integer>();
		for (int j = 0; j < 4; j++) {
			for (int i = 1; i <= 27; i++) {
				pais.add(i);
			}
			pais.add(32);
		}
		// 2.洗牌
		Collections.shuffle(pais);
		return pais;
	}

	/**
	 * 给手牌排序
	 * 
	 * @param pais
	 * @return
	 */
	public static List<Integer> paiXu(List<Integer> pais) {
		Collections.sort(pais);
		return pais;
	}

	/**
	 * 有一个玩家有吃碰杠,就不是三家闭
	 * @param room
	 * @param ps
	 * @return
	 */
	public static boolean sanJiaBi(List<Player> ps){
		for (int i = 0; i < ps.size(); i++) {
			if(ps.get(i).getIsHu()){
				continue;
			}
			List<Action> actionList = ps.get(i).getActionList();
			if(actionList != null && actionList.size() != 0){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 删除用户指定的一张牌
	 * 
	 * @param currentPlayer
	 * @return
	 */
	public static void removePai(Player currentPlayer, Integer action) {
		Iterator<Integer> pai = currentPlayer.getCurrentMjList().iterator();
		while (pai.hasNext()) {
			Integer item = pai.next();
			if (item == action) {
				pai.remove();
				break;
			}
		}
	}

	/**
	 * 
	 * @param room
	 *            房间
	 * @param currentPlayer
	 *            当前操作的玩家
	 * @return 返回需要通知的操作的玩家ID
	 */
	public static Long nextActionUserId(RoomResp room, Long lastUserId) {
		Long[] playerIds = room.getPlayerIds();

		for (int i = 0; i < playerIds.length; i++) {
			if (lastUserId == playerIds[i]) {
				if (i == playerIds.length - 1) { // 如果是最后 一个,则取第一个.
					return playerIds[0];
				} else {
					return playerIds[i + 1];
				}
			}
		}
		return -100l;
	}

	/**
	 * 定混牌//上滚定混
	 * 
	 * @param pais
	 *            第一次发牌后剩余的牌
	 * @return
	 */
	public static Integer dingHunPai(List<Integer> pais,RoomResp room) {
		List<Integer> faPai = faPai(pais,1);
		Integer hunPai = faPai.get(0);
		room.setDingHunPai(hunPai);
		if (hunPai == 32) {// 红中
			hunPai = 32;
		} else if (hunPai % 9 == 0) {// 上滚定混
			hunPai = hunPai - 8;
		} else { // 正常混牌
			hunPai = hunPai + 1;
		}
		return hunPai;
//		room.setDingHunPai(27);
//		return 19;
	}
 

	/**
	 * 干里干胡牌规则 1,必须有幺九牌 2,要求三门齐或清一色(红中不算们,单门+红中胡牌算清一色) 3,必须有刻牌(三个一样,或者四个一样)
	 * 4,必须开门才能胡牌(吃,碰,杠有一个算开门) 5,每一局只能胡一个玩家. 6,胡的优先级(胡>杠>碰>吃)
	 * 
	 *
	 * @return true 为可以胡牌牌型
	 */

	public static boolean checkHuRule(Player p,RoomResp room ,Integer pai,Integer type) {
		List<Integer> newList = getNewList(p.getCurrentMjList());
		if(type == Cnst.CHECK_TYPE_ZIJIMO){
			pai = null;
		}
		if(pai != null){
			newList.add(pai); //检测要带上别人打出的牌
		}
		if(!isKaiMen(p)){
			return false;
		}
		if(!checkYiJiu(p,newList,room.getHunPai())){
			return false;
		}
		if ((isQingYiSe(p,room,newList) || isSanMenQi(p,room,newList)) && isKePai(p,room.getHunPai(),newList) ) {
			return true;
		}
		return false;
	}

	/**
	 * 胡牌必须有幺九.
	 * @param p
	 * @param list
	 * @return
	 */
	private static boolean checkYiJiu(Player p, List<Integer> list,Integer hun) {
		List<Integer> shouPai = getNoHunList(list,hun);
		//检测手牌是否有1-9
		for (int i = 0; i < shouPai.size(); i++) {
			if (shouPai.get(i) == 1  || shouPai.get(i) == 10 || shouPai.get(i) == 19 || shouPai.get(i) == 9 || shouPai.get(i) == 18  || shouPai.get(i) == 27 || shouPai.get(i) == 32 ) {
				if(shouPai.get(i) != hun){
					return true;	
				}
			}
		}
		//判断有动作的牌类型是否相同
		List<Action> actionList = p.getActionList();
		if(actionList.size()>0){
			for (Action action : actionList) {
				if(Cnst.ACTION_YI_JIU.contains(action.getActionId())){
					return true;
				}
			}
		}
		return false;
		//检测手里有没有混
//		Integer hunNum = hunNum(p, hun);
//		if(hunNum == null || hunNum == 0){
//			return false;
//		}
//		List<Integer> newList = getNewList(list);
//		Iterator<Integer> it = newList.iterator();
//		while(it.hasNext()){
//			Integer x = it.next();
//			if(x == hun){
//				it.remove();
//			}
//		}
//		for (Integer i : Cnst.PAI_YI_JIU) {
//			newList.add(i);
//			int[] checkHu = getCheckHuPai(newList, null);
//			checkHu[33] = hunNum - 1;
//			if(Hulib.getInstance().get_hu_info(checkHu,34,33)){
//				//检测是碰碰胡 或 平胡
//				if(isPengPengHu(p, newList, hunNum-1)){
//					p.getFanShu().add(Cnst.YAOJIUPENGPENGHU);
//				}else{
//					p.getFanShu().add(Cnst.BUSHIYAOJIUPENGPENGHU);
//				}
//				return true;
//			}else{
//				it = newList.iterator();
//				a:while(it.hasNext()){
//					Integer x = it.next();
//					if(x == i){
//						it.remove();//!!!
//						break a;
//					}
//				}
//			}
//			
//		}
//		return false;
	}
	
	public static boolean isShouBaYi(Player p,Integer type){
		if(type == Cnst.CHECK_TYPE_BIERENCHU){
			if(p.getCurrentMjList().size() <= 4){
				return false;
			}
			return true;
		}
		if(type == Cnst.CHECK_TYPE_ZIJIMO || type == Cnst.CHECK_TYPE_HAIDIANPAI){
			if(p.getCurrentMjList().size() <= 5){
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * 干里干 牌型是否是清一色 红中不算门 ,单门+红中胡牌算是清一色
	 * 
	 * @param p玩家
	 * @return
	 */
	public static boolean isQingYiSe(Player p , RoomResp room ,List<Integer> list) {
		Integer leixing=0;
		Boolean needcheck=false;
		List<Integer> newList = getNewList(list);
		Iterator<Integer> iter1 = newList.iterator();
		while (iter1.hasNext()) {
			Integer item = iter1.next();
			if (item == room.getHunPai()) {
				iter1.remove();
			}
		}
		Collections.sort(newList);
		Integer pai = newList.get(0);
		leixing=(pai-1)/9;
		if(leixing==3){//单调红中，只看吃碰杠类型就Ok
			needcheck=true;
		}else{//不是单调红中
			for (Integer shouPai : newList) {
				//红中跳出
				if((shouPai-1)/9==3){
					continue;
				}
				//要检测的类型不再相同
				if(leixing!=(shouPai-1)/9){
					return false;
				}
			}
		}
		//判断有动作的牌类型是否相同
		Integer extra=0;
		List<Action> actionList = p.getActionList();
		if(actionList.size()>0){
			if(needcheck){//绝对不会大于3了，因为红中只能碰一次
				leixing=(actionList.get(0).getExtra()-1)/9;
				needcheck=false;
			}
			for (Action action : actionList) {
				extra = action.getExtra();
				if(extra>=28){
					continue;
				}else{
					if(leixing!=(extra-1)/9){
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * 检测动作集合
	 * @param p
	 * @param pai
	 * @param room
	 * @param type
	 * @param checkChi 自己打的牌true,不提示吃.
	 * @return
	 */
	public static List<Integer> checkActionList(Player p, Integer pai, RoomResp room,Integer type,Boolean checkChi) {
		Integer hunPai = room.getHunPai();
		List<Integer> actionList = new ArrayList<Integer>();

		Boolean isShouBaYi = isShouBaYi(p, type);
		if(Cnst.CHECK_TYPE_HAIDIANPAI == type){
			if (checkHuRule(p,room,pai,Cnst.CHECK_TYPE_ZIJIMO)) {
				int[] huPaiZu = getRemoveLastPai(p.getCurrentMjList(), pai);
				 if (Hulib.getInstance().get_hu_info(huPaiZu, pai-1, hunPai - 1)) { //FIXME 把手牌转为34位数组
					actionList.add(500);
				 }
			}
			return actionList;
		}
		
		if (isShouBaYi && checkChi && type != Cnst.CHECK_TYPE_ZIJIMO  && checkChi(p, pai,hunPai)) {
			List<Integer> c = chi(p, pai,hunPai);
			actionList.addAll(c);
		}
		if (isShouBaYi && type != Cnst.CHECK_TYPE_ZIJIMO  && checkPeng(p, pai,hunPai)) {
			Integer peng = peng(p, pai);
			actionList.add(peng);
		}
		//1,不是自摸,检测别人出牌的时候,能不能点杠.
		if (isShouBaYi && Cnst.CHECK_TYPE_ZIJIMO != type && checkGang(p, pai)) {
			Integer gang = gang(p, pai,false);
			actionList.add(gang);
		}
		//2,自摸的时候,检测能不能暗杠.
		if(isShouBaYi && Cnst.CHECK_TYPE_ZIJIMO == type ){
			List<Integer> checkAnGang = checkAnGang(p,hunPai);
			for (int i = 0; i < checkAnGang.size(); i++) {
				actionList.add(checkAnGang.get(i));
			}
		}
		//3,自摸的时候,检测能不能碰杠
		if(Cnst.CHECK_TYPE_ZIJIMO == type){
			List<Integer> pengGang = checkPengGang(p, pai);
			if(pengGang.size() != 0){
				actionList.addAll(pengGang);
			}
			
		}
		
		//不能杠开
		if(room.getLastAction() == null || (room.getLastAction()>=1 && room.getLastAction()<91)){
			if (checkHuRule(p,room,pai,type)) {
				int[] huPaiZu = new int[34];
				if(type == Cnst.CHECK_TYPE_BIERENCHU){
					 huPaiZu = getCheckHuPai(p.getCurrentMjList(), pai);
					 if (Hulib.getInstance().get_hu_info(huPaiZu, 34, hunPai - 1)) { //FIXME 把手牌转为34位数组
							actionList.add(500);
						}
				}else{
					 huPaiZu = getRemoveLastPai(p.getCurrentMjList(), pai);
					 if (Hulib.getInstance().get_hu_info(huPaiZu, pai-1, hunPai - 1)) { //FIXME 把手牌转为34位数组
							actionList.add(500);
						}
				}				
				
			}
		}		
		if (actionList.size() != 0) {
			actionList.add(0);
		}else{
			//没有动作 只能出牌
			if(type == Cnst.CHECK_TYPE_ZIJIMO){
				actionList.add(501);
			}		
		}		
		return actionList;
	}

	/**
	 * 检测能不能碰完以后再开杠.
	 * @param p
	 * @return
	 */
	private static List<Integer> checkPengGang(Player p, Integer pai) {
		List<Action> actionList = p.getActionList();//统计用户所有动作 (吃碰杠等)
		List<Integer> newList = getNewList(p.getCurrentMjList());
		List<Integer> gangList = new ArrayList<Integer>();
		for (int i = 0; i < actionList.size(); i++) {
			if(actionList.get(i).getType() == 2){
				for(int m=0;m<newList.size();m++){
					if(newList.get(m) == actionList.get(i).getExtra()){
						gangList.add(newList.get(m)+90);
					}
				}
			}
		}
		return gangList;
	}

	/**
	 * 混牌的数量
	 * 
	 * @return
	 */
	public static Integer hunNum(Player p, Integer pai) {
		Integer num = 0;
		for (int i = 0; i < p.getCurrentMjList().size(); i++) {
			if (p.getCurrentMjList().get(i) == pai) {
				num = num + 1;
			}
		}
		return num;
	}

	/**
	 * 干里干 牌型是否有三门齐(万,饼,条) 此方法稍加变形亦可检测是否是清一色
	 * 
	 * @param p
	 * @return
	 */
	public static boolean isSanMenQi(Player p,RoomResp room,List<Integer> newList) {
		List<Integer> shouPai = getNoHunList(newList, room.getHunPai());
		boolean b = false;
		boolean c = false;
		boolean d = false;
		for (int i = 0; i < shouPai.size(); i++) {
			if (shouPai.get(i) <= 9) {
				b = true;
			}
			if (shouPai.get(i) >= 10 && shouPai.get(i) <= 18) {
				c = true;
			}
			if (shouPai.get(i) >= 19 && shouPai.get(i) <= 27) {
				d = true;
			}
		}
		if (b && c && d) {
			return true;
		}
		
		Integer extra=0;
		List<Action> actionList = p.getActionList();
		for (Action action : actionList) {
			extra = action.getExtra();
			if (extra <= 9) {
				b = true;
			}
			if (extra >= 10 && extra <= 18) {
				c = true;
			}
			if (extra >= 19 && extra <= 27) {
				d = true;
			}
		}
		if (b && c && d) {
			return true;
		}
		return false;
	}

	/**
	 * 牌型是否有刻牌(三个一样或者四个一样)
	 * 
	 * @param p
	 * @return true 是
	 */
	public static boolean isKePai(Player p,Integer hunPai,List<Integer> newList) {
		//检测动作里面是否有刻
		List<Action> actionList = p.getActionList();
		//1吃   2碰  3点杠 4碰杠 5暗杠 
		for (Action action : actionList) {
			if(action.getType()!=1){
				return true;
			}
		}
		//检测手中有没有刻
		Set<Integer> distinct=new HashSet<Integer>();
		for (Integer integer : newList) {
			distinct.add(integer);
		}
		//手牌中是否有3张,这3张移除必须能胡才行 比如12333
		Integer hunNum = hunNum(p, hunPai);
		int num=0;
		for (Integer distinctPai : distinct) {
			num=0;
			for (Integer p1 : newList) {
				if(distinctPai.equals(p1)){
					num++;
				}
			}
			if(num>=3){					
				int[] huPaiZu = getCheckHuPai(newList,null); 
				//将这3张牌移除
				huPaiZu[distinctPai-1] = num - 3;
				if (Hulib.getInstance().get_hu_info(huPaiZu, 34, hunPai - 1)) {
					return true;
				}
			}else if(hunNum + num >=3){
				int[] huPaiZu = getCheckHuPai(newList,null);
				huPaiZu[hunPai - 1] =hunNum -  (3 - num);
				huPaiZu[distinctPai-1] = 0;
				if (Hulib.getInstance().get_hu_info(huPaiZu, 34, hunPai - 1)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 必须开门才能胡牌(吃,碰,杠)的一种.
	 * 
	 * @param p
	 * @return 是 true
	 */
	public static boolean isKaiMen(Player p) {
		if(p.getActionList() == null || p.getActionList().size() == 0){
			return false;
		}
		int num = 0;
		for(Action ac:p.getActionList()){
			if(ac.getType()!=Cnst.ACTION_TYPE_ANGANG){
				num = num + 1;
			}
		}
		if(num > 0 ){
			return true;
		}
		return false;
	}

	/***
	 * 根据出的牌 设置下个动作人和玩家
	 * @param players
	 * @param room
	 * @param pai
	 */
	public static void getNextAction(List<Player> players, RoomResp room, Integer pai){
		Integer maxAction = 0;
		Long nextActionUserId = -1L;
		List<Integer> nextAction = new ArrayList<Integer>();
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
		Long xiaYiJia = playIds[index];
		for(Player p:players){
			if(!room.getGuoUserIds().contains(p.getUserId())){
				//玩家没点击过 或者不是 出牌的人  吃只检测下个人
				List<Integer> checkActionList;
				if(p.getUserId().equals(xiaYiJia)){
					checkActionList = checkActionList(p, pai, room,Cnst.CHECK_TYPE_BIERENCHU,true);
				}else{
					checkActionList = checkActionList(p, pai, room,Cnst.CHECK_TYPE_BIERENCHU,false);
				}
				
				if(checkActionList.size() == 0){
					//玩家没动作 
					room.getGuoUserIds().add(p.getUserId());
				}else{
					Collections.sort(checkActionList);
					if(checkActionList.get(checkActionList.size()-1) > maxAction){
						nextActionUserId = p.getUserId();
						nextAction = checkActionList;
						maxAction = checkActionList.get(checkActionList.size()-1);
					}
				}
			}
		}
		//如果都没可执行动作 下一位玩家请求发牌
		if(maxAction == 0){
			nextAction.add(-1);
			room.setNextAction(nextAction);
			//取到上个出牌人的角标 下一位来发牌
			room.setNextActionUserId(xiaYiJia);
		}else{
			room.setNextAction(nextAction);
			room.setNextActionUserId(nextActionUserId);
		}
	
	}

	/**
	 * 检查玩家能不能碰
	 * 
	 * @param p
	 * @param Integer
	 *            peng 要碰的牌
	 * @return
	 */
	public static boolean checkPeng(Player p, Integer peng,Integer hunPai) {
		int num = 0;
		for (Integer i : p.getCurrentMjList()) {
			if(i == peng){
				num++;
			}
		}
		if (num >= 2) {
			return true;
		}
		return false;
	}

	/**
	 * //与吃的那个牌能组合的List
	 * @param p
	 * @param chi
	 * @return
	 */
	public static List<Integer> reChiList(Integer action ,Integer chi){
		ArrayList<Integer> arrayList = new ArrayList<Integer>();
		for (int i = 35; i <= 56; i++) {
			if(i == action ){
				int[] js = Cnst.chiMap.get(action);
				for (int j = 0; j < js.length; j++) {
					if(js[j] != chi){
						arrayList.add(js[j]);
					}
				}
			}
		}
		return arrayList; 
	}
	
	/**
	 * 执行动作吃!
	 * 返回原本手里的牌
	 * @param p
	 * @param chi
	 * @return
	 */
	public static List<Integer> chi(Player p, Integer chi, Integer hunPai) {
		List<Integer> shouPai = getNoHunList(p.getCurrentMjList(), hunPai);
		Set<Integer> set = new HashSet<Integer>();
		List<Integer> reList = new ArrayList<Integer>();
		boolean a = false; // x<x+1<x+2
		boolean b = false; // x-1<x<x+1
		boolean c = false; // x-2<x-1<x

		// 万
		if (chi < 10) { // 基数34
			List<Integer> arr = new ArrayList<Integer>();
			arr.add(chi + 1);
			arr.add(chi + 2);
			if (shouPai.containsAll(arr)) {
				a = true;
			}
			List<Integer> arr1 = new ArrayList<Integer>();
			arr1.add(chi - 1);
			arr1.add(chi + 1);
			if (shouPai.containsAll(arr1)) {
				b = true;
			}
			List<Integer> arr2 = new ArrayList<Integer>();
			arr2.add(chi - 1);
			arr2.add(chi - 2);
			if (shouPai.containsAll(arr2)) {
				c = true;
			}

			if (a && chi != 9 && chi != 8) {
				set.add(34 + chi);
			}
			if (b && chi != 9) {
				set.add(33 + chi);
			}
			if (c) {
				set.add(32 + chi);
			}

			// 饼
		} else if (chi >= 10 && chi <= 18) { // 基数32
			List<Integer> arr = new ArrayList<Integer>();
			arr.add(chi + 1);
			arr.add(chi + 2);
			if (shouPai.containsAll(arr)) {
				a = true;
			}
			List<Integer> arr1 = new ArrayList<Integer>();
			arr1.add(chi - 1);
			arr1.add(chi + 1);
			if (shouPai.containsAll(arr1)) {
				b = true;
			}
			List<Integer> arr2 = new ArrayList<Integer>();
			arr2.add(chi - 1);
			arr2.add(chi - 2);
			if (shouPai.containsAll(arr2)) {
				c = true;
			}
			if (a & chi != 18 && chi != 17) {
				set.add(32 + chi);
			}
			if (b && chi != 10 && chi != 18) {
				set.add(31 + chi);
			}
			if (c && chi != 10 && chi != 11) {
				set.add(30 + chi);
			}
			// 条
		} else if (chi >= 19 && chi <= 27) { // 基数30
			List<Integer> arr = new ArrayList<Integer>();
			arr.add(chi + 1);
			arr.add(chi + 2);
			if (shouPai.containsAll(arr)) {
				a = true;
			}
			List<Integer> arr1 = new ArrayList<Integer>();
			arr1.add(chi - 1);
			arr1.add(chi + 1);
			if (shouPai.containsAll(arr1)) {
				b = true;
			}
			List<Integer> arr2 = new ArrayList<Integer>();
			arr2.add(chi - 1);
			arr2.add(chi - 2);
			if (shouPai.containsAll(arr2)) {
				c = true;
			}
			if (a & chi != 26 && chi != 27) {
				set.add(30 + chi);
			}
			if (b && chi != 19 && chi != 27) {
				set.add(29 + chi);
			}
			if (c && chi != 19 && chi != 20) {
				set.add(28 + chi);
			}
		}
		reList.addAll(set);
		return reList;
	}
	/**
	 * 执行动作杠
	 * 
	 * @param p
	 * @param gang
	 * @return
	 */
	public static Integer gang(Player p, Integer gang, Boolean pengGang) {
		List<Integer> shouPai = p.getCurrentMjList();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		
		if(pengGang){
			List<Action> actionList = p.getActionList();//统计用户所有动作 (吃碰杠等)
			for (int i = 0; i < actionList.size(); i++) {
				if(actionList.get(i).getType() == 2 && actionList.get(i).getExtra() == gang){
					return 90 + gang;
				}
			}
		}

		for (Integer item : shouPai) {
			if (map.containsKey(item)) {
				map.put(item, map.get(item).intValue() + 1);
			} else {
				map.put(item, new Integer(1));
			}
		}

		Iterator<Integer> keys = map.keySet().iterator();
		while (keys.hasNext()) {
			Integer key = keys.next();
			if (map.get(key).intValue() == 3) { // 控制有几个重复的
				// System.out.println(key + "有重复的:" + map.get(key).intValue() +
				// "个 ");
				if (key == gang) {
					return 90 + gang;
				}
			}
		}

		return -100;
	}

	/**
	 * 执行动作碰
	 * 
	 * @param p
	 * @param peng
	 * @return 行为编码
	 */
	public static Integer peng(Player p, Integer peng) {
		return 56 + peng;
	}

	/**
	 *  * 检测玩家能不能吃.10 与19特殊处理
	 * @param p
	 * @param chi
	 * @param hunPai 不能吃
	 * @return
	 */
	public static boolean checkChi(Player p, Integer chi,Integer hunPai) {
		List<Integer> list = getNoHunList(p.getCurrentMjList(), hunPai);
		boolean isChi = false;
		List<Integer> arr = new ArrayList<Integer>();
		arr.add(chi + 1);
		arr.add(chi + 2);
		if (list.containsAll(arr)) {
			isChi = true;
		}
		List<Integer> arr1 = new ArrayList<Integer>();
		List<Integer> arr2 = new ArrayList<Integer>();
		if (chi != 10 && chi != 19) {
			arr1.add(chi - 1);
			arr1.add(chi + 1);
			if (list.containsAll(arr1)) {
				isChi = true;
			}
			arr2.add(chi - 1);
			arr2.add(chi - 2);
			if (list.containsAll(arr2)) {
				isChi = true;
			}
		}
		return isChi;
	}

	/**
	 * 执行暗杠
	 * 
	 * @param p
	 * @return 返回杠的牌
	 */
	public static Integer anGang(Player p) {
		List<Integer> shouPai = p.getCurrentMjList();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();

		for (Integer item : shouPai) {
			if (map.containsKey(item)) {
				map.put(item, map.get(item).intValue() + 1);
			} else {
				map.put(item, new Integer(1));
			}
		}

		Iterator<Integer> keys = map.keySet().iterator();
		Integer gang = 0;
		while (keys.hasNext()) {
			Integer key = keys.next();
			if (map.get(key).intValue() == 4) { // 控制有几个重复的
				// System.out.println(key + "有重复的:" + map.get(key).intValue() +
				// "个 ");
				gang = key;
			}
		}

		Iterator<Integer> iter1 = p.getCurrentMjList().iterator();
		while (iter1.hasNext()) {
			Integer item = iter1.next();
			if (item == gang) {
				iter1.remove();
			}
		}
		return gang + 90;
	}

	/**
	 * 检查能不能暗杠
	 * 
	 * @param p
	 * @param gang
	 * @return
	 */
	public static List<Integer> checkAnGang(Player p,Integer hunPai) {
		List<Integer> anGangList = new ArrayList<Integer>();
		List<Integer> shouPai = p.getCurrentMjList();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();

		for (Integer item : shouPai) {
			if (map.containsKey(item)) {
				map.put(item, map.get(item).intValue() + 1);
			} else {
				map.put(item, new Integer(1));
			}
		}

		Iterator<Integer> keys = map.keySet().iterator();
		while (keys.hasNext()) {
			Integer key = keys.next();
			if (map.get(key).intValue() == 4 && key != hunPai) { // 控制有几个重复的
				 anGangList.add(key+90);
			}
		}
		return anGangList;
	}

	/**
	 * 检测玩家能不能杠
	 * 1,明杠,2暗杠,3 点杠
	 * @param p
	 * @return
	 */
	public static boolean checkGang(Player p, Integer gang) {
		List<Integer> shouPai = p.getCurrentMjList();
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();

		for (Integer item : shouPai) {
			if (map.containsKey(item)) {
				map.put(item, map.get(item).intValue() + 1);
			} else {
				map.put(item, new Integer(1));
			}
		}

		Iterator<Integer> keys = map.keySet().iterator();
		while (keys.hasNext()) {
			Integer key = keys.next();
			if (map.get(key).intValue() == 3) { // 控制有几个重复的
				// System.out.println(key + "有重复的:" + map.get(key).intValue() +
				// "个 ");
				if (key == gang) {
					// TODO
					// System.out.println("可以杠的牌是:"+key);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 
	 * @param mahjongs
	 *            房间内剩余麻将的组合
	 * @param num
	 *            发的张数
	 * @return
	 */
	public static List<Integer> faPai(List<Integer> mahjongs, Integer num) {
		if (mahjongs.size() == 8) {
			return null;
		}
		List<Integer> result = new ArrayList<>();
		for (int i = 0; i < num; i++) {
			result.add(mahjongs.get(i));
			mahjongs.remove(i);
		}
		return result;
	}
	
	//测试发牌
	public static List<Integer> faPai(Integer num){
		List<Integer> result = new ArrayList<>();
		result.add(27);
		result.add(27);
		result.add(27);
		result.add(3);
		result.add(4);
		result.add(5);
		result.add(6);
		result.add(7);
		result.add(10);
		result.add(11);
		result.add(12);
		result.add(16);
		result.add(16);
		if(num == 13){
			return result;
		}
		result.add(2);
		return result;
	}

	/**
	 * 牌型是不是碰碰胡(全是刻牌的牌型)
	 * 
	 * @param p
	 * @param hunNum 
	 * @param newList 
	 * @return
	 */
	public static boolean isPengPengHu(Player p, List<Integer> newList, Integer hunNum) {
		//检测动作里面是否有刻
		List<Action> actionList = p.getActionList();
		//1吃   2碰  3点杠 4碰杠 5暗杠 
		for (Action action : actionList) {
			if(action.getType()==1){
				return false;
			}
		}
		//检测手牌是不是都是刻
		int[] checkHuPai = getCheckHuPai(newList,null);
		int twoNum=0;
		int oneNum=0;
		for (Integer integer : checkHuPai) {
			if(integer==1){
				oneNum++;
			}else if(integer==2){
				twoNum++;
			}else if(integer==3){
			}else if(integer==4){
				return false;
			}
		}
		//两张的需要1张混，1张需要两张混,但是将减少一张混的需求
		if((twoNum+oneNum*2-1)<=hunNum){
			return true;
		}
		
		return false;
	}
	/**
	 * check peng
	 * @param newList
	 * @param hunNum
	 * @return
	 */
	public static boolean isRally(List<Integer> newList, Integer hunNum) {
		//检测手牌是不是都是刻
		int[] checkHuPai = getCheckHuPai(newList,null);
		int twoNum=0;
		int oneNum=0;
		for (Integer integer : checkHuPai) {
			if(integer==1){
				oneNum++;
			}else if(integer==2){
				twoNum++;
			}else if(integer==3){
			}else if(integer==4){
				return false;
			}
		}
		//两张的需要1张混，1张需要两张混,但是将减少一张混的需求
		if((twoNum+oneNum*2-1)<=hunNum){
			return true;
		}
		return false;
	}
	
	
	
	/**
	 * 返回一个新的集合
	 * @param old
	 * @return
	 */
	static List<Integer> getNewList(List<Integer> old) {
		List<Integer> newList = new ArrayList<Integer>();
		if (old != null && old.size() > 0) {
			for (Integer pai : old) {
				newList.add( pai );
			}
		}
		return newList;
	}

	/**
	 * 干里干检测胡牌的类型. 以及番数统计
	 * 
	 * @param p
	 * @return 返回的是分数
	 */
	public static Integer checkHuInfo(List<Player> players,RoomResp room) {
		Player p = null;
		for (Player player : players) {
			if (player.getIsHu()) {
				player.setHuNum(player.getHuNum() + 1);
				p = player;
			}
		}
		//番信息
		List<Integer> fanInfo = new ArrayList<Integer>();
		Integer hunNum = 0;
		List<Integer> newList = getNewList(p.getCurrentMjList());
		for(Integer i:p.getCurrentMjList()){
			if(i == room.getHunPai()){
				hunNum++;
			}
		}
		Iterator<Integer> it = newList.iterator();
		while(it.hasNext()){
			Integer x = it.next();
		    if(x==room.getHunPai()){
		        it.remove();
		    }
		}
		
		Integer fan = 1;
		
		// 碰碰胡
//		if(p.getFanShu()!=null && p.getFanShu().contains(Cnst.YAOJIUPENGPENGHU)){
//			p.setPengPengHuNum(p.getPengPengHuNum() + 1);
//			fan = fan + 1;
//			fanInfo.add(Cnst.PENGPENGHU);	
//		}else{
			if(p.getFanShu() != null && p.getFanShu().size() == 0){
				if (isPengPengHu(p,newList,hunNum)) {
					p.setPengPengHuNum(p.getPengPengHuNum() + 1);
					fan = fan + 1;
					fanInfo.add(Cnst.PENGPENGHU);
				}
			}
//		}
		
		//三家闭
		if(MahjongUtils.sanJiaBi(players)){
			p.setSanJiaBiNum(p.getSanJiaBiNum() + 1);
			fan = fan + 1;
			fanInfo.add(Cnst.SANJIABI);
		}
		// 清一色
		if (isQingYiSe(p,room,newList)) {
			p.setQingYiSeNum(p.getQingYiSeNum() + 1); 
			fan = fan + 1;
			fanInfo.add(Cnst.QINGYISE);
		}
		// 庄家输赢都加1
		if (p.getUserId().equals(room.getZhuangId())) {
			fanInfo.add(Cnst.ZHUANGJIA);
		}
		// 自摸
		if (p.getIsZiMo()) {
			p.setZimoNum(p.getZimoNum() + 1);
			fan = fan + 1;
			fanInfo.add(Cnst.ZIMO);
		}
		if(!p.getIsZiMo()){
			fanInfo.add(Cnst.DIANPAO);
		}
		   // 干里干
		if ((hunNum == 0 && p.getDuiHunNum() == 1) || (p.getDuiHunNum() == 2 && xiHu(p,hunNum))) {
			fanInfo.add(Cnst.GANLIGAN);
			p.setFanShu(fanInfo);
			return fan + 2;
			// 三干
		} else if ((hunNum == 0 && p.getDuiHunNum() == 2) || (p.getDuiHunNum() == 3 && xiHu(p,hunNum))) {
			fanInfo.add(Cnst.SANGAN);
			p.setFanShu(fanInfo);
			return fan + 3;
			// 四干
		} else if (hunNum == 0 && p.getDuiHunNum() == 3) {
			fanInfo.add(Cnst.SIGAN);
			p.setFanShu(fanInfo);
			return fan + 4;
			// 五干
		} else if (hunNum == 0 && p.getDuiHunNum() == 4) {
			fanInfo.add(Cnst.WUGAN);
			p.setFanShu(fanInfo);
			return fan + 5;
			// 干胡
		}else if (hunNum == 0 || (p.getDuiHunNum() == 1 && xiHu(p,hunNum))) {
			fanInfo.add(Cnst.GANHU);
			p.setFanShu(fanInfo);
			return fan + 1;
			// 稀胡
		}else if (hunNum != 0 ) {
			fanInfo.add(Cnst.XIHU);
			p.setFanShu(fanInfo);
			return fan;
		} 
		p.setFanShu(fanInfo);
		return fan;
	}

	/**
	 * 是不是稀胡牌型
	 * 
	 * @param p
	 * @return
	 */
	public static boolean xiHu(Player p,Integer hunNum) {
		if (hunNum != 0) {
			return true;
		}
		return false;
	}
	/**
	 * 从牌桌上,把玩家吃碰杠的牌移除.
	 * @param room
	 * @param players
	 */
	
	public static void removeCPG(RoomResp room, List<Player> players) {
		Player currentP = null;
		for (Player p : players) {
			if(p.getUserId().equals(room.getLastChuPaiUserId())){
				currentP = p;
				List<Integer> chuList = p.getChuList();
				Iterator<Integer> iterator = chuList.iterator();
				while(iterator.hasNext()){
					Integer pai = iterator.next();
					if(room.getLastChuPai() == pai ){
						iterator.remove();
						break;
					}
				}
			}
		}
		RedisUtil.updateRedisData(null, currentP);
	}
	
	/***
	 * 移除动作手牌 
	 * @param currentMjList
	 * @param chi
	 * @param action
	 * @param type
	 */
	public static void removeActionMj(List<Integer> currentMjList,List<Integer> chi,Integer action,Integer type){
		Iterator<Integer> it = currentMjList.iterator(); //遍历手牌,删除碰的牌
		switch (type) {
		case Cnst.ACTION_TYPE_CHI:
			int chi1 = 0;
			int chi2 = 0;
			a : while(it.hasNext()){
					Integer x = it.next();
					if(x == chi.get(0) && chi1 == 0){
						it.remove();
						chi1 = 1 ;
					}
					if(x == chi.get(1) && chi2 == 0){
						it.remove();
						chi2 = 1;
					}
					if(chi1 == 1 && chi2 == 1){
						break a;
					}
				}		
			break;
		case Cnst.ACTION_TYPE_PENG:
			int num = 0;
			while(it.hasNext()){
				Integer x = it.next();
			    if(x==action-56){
			        it.remove();
			        num = num + 1;
			        if(num == 2){
			        	break;
			        }
			    }
			}
			break;
		case Cnst.ACTION_TYPE_ANGANG:
			List<Integer> gangPai = new ArrayList<Integer>();
			gangPai.add(action-90);
			currentMjList.removeAll(gangPai);
			break;
		case Cnst.ACTION_TYPE_PENGGANG:
			gangPai = new ArrayList<Integer>();
			gangPai.add(action-90);
			currentMjList.removeAll(gangPai);
			break;
		case Cnst.ACTION_TYPE_DIANGANG:
			gangPai = new ArrayList<Integer>();
			gangPai.add(action-90);
			currentMjList.removeAll(gangPai);
			break;
		default:
			break;
		}
	}
	/***
	 * 获得 检测胡牌的 34位数组 包括摸得或者别人打的那张
	 * @param currentList
	 * @param pai
	 * @return
	 */
	public static int[] getCheckHuPai(List<Integer> currentList,Integer pai){
		int[] checkHuPai = new int[34];
		List<Integer> newList = getNewList(currentList);
		if(pai!=null){
			newList.add(pai);
		}
		for(int i=0;i<newList.size();i++){
			int a = checkHuPai[newList.get(i) - 1];
			checkHuPai[newList.get(i) - 1] = a + 1;
		}
		return checkHuPai;
	}

	/***
	 * 获得 检测胡牌的 34位数组 不包括摸得或者别人打的那张
	 * @param currentList
	 * @param pai
	 * @return
	 */
	public static int[] getRemoveLastPai(List<Integer> currentList,Integer pai){
		int[] checkHuPai = new int[34];
		Boolean hasRemove = false; 
		for(int i=0;i<currentList.size();i++){
			if(currentList.get(i) == pai && !hasRemove){
				hasRemove = true;
				continue;
			}
			int a = checkHuPai[currentList.get(i) - 1];
			checkHuPai[currentList.get(i) - 1] = a + 1;
		}
		return checkHuPai;
	}
	
	/***
	 * 移除混牌 做判断用
	 * @param currList 手牌
	 * @param hunPai 混牌
	 * @return
	 */
	public static List<Integer> getNoHunList(List<Integer> currList,Integer hunPai){
		List<Integer> list = new ArrayList<Integer>();
		for(Integer i:currList){
			if(i == hunPai){
				continue;
			}
			list.add(i);
		}
		return list;
	}
	
	
	
}
