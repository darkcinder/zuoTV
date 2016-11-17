package com.mtv.room

import com.alibaba.fastjson.JSON
import com.mtv.Room
import grails.transaction.Transactional
import org.jsoup.Jsoup

@Transactional
class HuYaRoomService extends SupportLoadRoom {

    /**
     *
     * @param platformFlag 平台标识
     */
    HuYaRoomService() {
        super("huYa")
    }

    /**
     * 刷新平台房间数据
     */
    @Override
    public List<List<Map>> loadData() {

        List pageList = []
        int pageCount = 1
        for(int a = 1; a <= pageCount; a++){
            def page = this.getPageObj(a)
            if(a == 1){
                pageCount = Math.ceil(Integer.parseInt(page.data.total) / 20d)
                log.info("${platformFlag}总条数:${page.data.total},总页数${pageCount}")
            }
            pageList << page.data.list
        }

        return pageList
    }

    @Override
    void saveRoom(Object obj) {
        List<List<Map>> pageList = (List<List<Map>>)obj
        Date lastUpdated = new Date()
        // 解析并保存数据
        pageList.each {
            List<Map> list = it
            list.each{
                // 查看如果是老数据则覆盖,新数据则新建
                String roomId = it.uid
                Room room = Room.findByPlatformAndFlag(platform, roomId)
                if(!room){
                    room = new Room(platform: platform, flag: roomId)
                }
                room.isOnLine = true
                room.name = it.introduction
                room.img = it.screenshot
                room.tag = it.gameFullName
                room.adNum = Long.parseLong(it.totalCount)
                room.anchor = it.nick
                room.url = "http://www.huya.com/" + it.privateHost
                room.lastUpdated = lastUpdated
                room.save()
            }
        }

        // 将平台下所有房间置为离线
        Room.executeUpdate("update Room r set r.isOnLine = false where r.platform = ? and r.lastUpdated < ?", [this.platform, lastUpdated])
    }

    public Object getPageObj(int pageIndex){
        String body = Jsoup.connect("http://www.huya.com/cache.php?m=Live&do=ajaxAllLiveByPage&page=${pageIndex}&pageNum=1")
                .timeout(60000)
                .header("User-Agent","Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2")
                .ignoreContentType(true).execute().body()
        return JSON.parse(body)
    }




}