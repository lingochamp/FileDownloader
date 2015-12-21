package com.liulishuo.filedownloader.demo;

/**
 * Created by Jacksgong on 12/19/15.
 */
public interface Constant {

    String[] BIG_FILE_URLS = {
            "http://7xjww9.com1.z0.glb.clouddn.com/20130221114001385.jpg",
            "http://dg.101.hk/1.rar",
            // 5m
            "http://mirror.internode.on.net/pub/test/5meg.test5",
            "http://mirror.internode.on.net/pub/test/5meg.test4",
            "http://mirror.internode.on.net/pub/test/5meg.test3",
            "http://mirror.internode.on.net/pub/test/5meg.test2",
            "http://mirror.internode.on.net/pub/test/5meg.test1",
            // 6m
            "http://download.chinaunix.net/down.php?id=10608&ResourceID=5267&site=1",
            // 6.8m
            "http://dlsw.baidu.com/sw-search-sp/soft/7b/33461/freeime.1406862029.exe",
            // 10m
            "http://mirror.internode.on.net/pub/test/10meg.test",
            "http://mirror.internode.on.net/pub/test/10meg.test1",
            "http://mirror.internode.on.net/pub/test/10meg.test2",
            "http://mirror.internode.on.net/pub/test/10meg.test3",
            "http://mirror.internode.on.net/pub/test/10meg.test4",
            "http://mirror.internode.on.net/pub/test/10meg.test5",
            // 20m
            "http://www.pc6.com/down.asp?id=72873",
            // 22m
            "http://113.207.16.84/dd.myapp.com/16891/2E53C25B6BC55D3330AB85A1B7B57485.apk?mkey=5630b43973f537cf&f=cf87&fsname=com.htshuo.htsg_3.0.1_49.apk&asr=02f1&p=.apk",
            // 34.2m
            "https://cx-ep.xiaojukeji.com/pkg//download/passenger/taxi/newhome/android/output/passenger_android_build_release/351/DiDi_passenger_taxi_newhome_build_351.apk?_t=hl6KNbYOuMLom9tRRV0U43DSMpjwXAXmhorB84U6ajlmmRigEsXBr9qEAjOeMJIVAajJAqwhrLMdpJaffBj4o6kgvZ/A3iUTlgkYq4RPdhzDWNJ1tIbHlqdeC9PEu5ISYCMfa%2BiRPyPJQ3c0qcLuhcNbmXtYp3v%2BvIn3vIWFNSM= ",
            // 206m
            "http://down.tech.sina.com.cn/download/d_load.php?d_id=49535&down_id=1&ip=42.81.45.159",
    };

    String[] URLS = {
            // 随机小资源一般不超过10
            "http://cdn-l.llsapp.com/connett/25183b40-22f2-0133-6e99-029df5130f9e",
            "http://cdn-l.llsapp.com/connett/c3115411-3669-466d-8ef2-e6c42c690303",
            "http://cdn-l.llsapp.com/connett/a55b4727-e228-410f-b44a-0385dbe9ab85",
            "http://cdn-l.llsapp.com/connett/7b6b5485-0d19-476c-816c-ff6523fae539",
            "http://cdn-l.llsapp.com/connett/33fa9155-c99a-407f-8d2c-82e9d17f4c32",
            "http://cdn-l.llsapp.com/connett/fe50a391-d111-44a9-9c2f-33aaaeec9186",
            "http://cdn.llsapp.com/crm_test_1449051526097.jpg",
            "http://cdn.llsapp.com/crm_test_1449554617476.jpeg",
            "http://cdn.llsapp.com/yy/image/3b0430db-5ff4-455c-9c8d-0213eea7b6c4.jpg",
            "http://cdn.llsapp.com/forum/image/ba80be187e0947f2b60c763a04910948_1446722022222.jpg",
            "http://cdn.llsapp.com/forum/image/NTNjMWQwMDAwMDAwMGQ0Zg==_1446122845.jpg",
            "http://cdn.llsapp.com/user_images/FEFC55C5-1E8F-45C6-AA4E-79FC79F97B6F",
            "http://cdn.llsapp.com/user_images/26ebf7deb8eb1f66056cbdac31aa18209d2f7daf_1436262740.jpg",
            "http://cdn.llsapp.com/yy/image/a1de0e33-c3f3-4795-b2b9-4dafbcf06bee.jpg",
            "http://cdn.llsapp.com/yy/image/cc4bc37d-ef77-4469-a8e9-2c70105a3f94.jpg",
            // 重复
            "http://cdn.llsapp.com/yy/image/cc4bc37d-ef77-4469-a8e9-2c70105a3f94.jpg",
            "http://cdn.llsapp.com/yy/image/dd72c879-b1c4-4fb9-b871-d57dfa3aa709.jpg",
            "http://cdn.llsapp.com/crm_test_1447220020113.jpg",
            "http://cdn.llsapp.com/crm_test_1447220428493.jpg",
            // 重复
            "http://cdn.llsapp.com/yy/image/a1de0e33-c3f3-4795-b2b9-4dafbcf06bee.jpg",
            "http://cdn.llsapp.com/forum/image/72e344b20d48432487389f8ad0dec163_1435047695818.png",
            "http://cdn.llsapp.com/forum/image/36d3070792b14633ad1f596c38f892e2_1435047020634.jpg",
            "http://cdn.llsapp.com/yy/image/5d8bfbd4-51b8-4fe6-ba01-4a5f37c478a6.jpg",
            "http://cdn.llsapp.com/forum/image/M2YwMWQwMDAwMDAwMTBmYw==_1440748066.jpg",
            "http://cdn.llsapp.com/forum/image/22f8389542734b05986c0b0dd8fd1735_1435230013392.jpg",
            "http://cdn.llsapp.com/forum/image/2e6b8f9676aa47228aad74dd37709b0e_1446202991820.jpg",
            "http://cdn.llsapp.com/forum/image/f82192fa9f764af396579e51afeb9aaf_1435049606128.jpg",
            "http://cdn.llsapp.com/forum/image/f74026981afa42e0b73a6983450deca1_1441780286505.jpg",
            "http://cdn.llsapp.com/357070051859561_1390016094611.jpg",
            "http://cdn.llsapp.com/forum/image/6f7a673ea1224019bf73bb2301f61b26_1435211914955.jpg",
            "http://cdn.llsapp.com/forum/image/a58b054f250e4237bd7d914c1feafc05_1435211918877.jpg",
            // 重复
            "http://cdn.llsapp.com/forum/image/f74026981afa42e0b73a6983450deca1_1441780286505.jpg",
            "http://cdn.llsapp.com/forum/image/432f360f3a1b4436b569c1a58c0dffe4_1435917578613.jpg",
            "http://cdn.llsapp.com/forum/image/a704f63a5b904961b71ea04b8a6aa36d_1397448248398.jpg",
            "http://cdn.llsapp.com/yy/image/52f4abdb-5f7f-46c2-9095-cce5fc09b296.png",
            "http://placekitten.com/580/320",
            "http://cdn.llsapp.com/forum/image/MWIwMWQwMDAwMDAwMTA2Yw==_1436253885.jpg",
            "http://cdn.llsapp.com/forum/image/2f003721ddb74ea1a84b2a6e603d6a44_1435046970863.jpg",
            "http://cdn.llsapp.com/crm_test_1447219868528.jpg",
            "http://cdn.llsapp.com/crm_test_1438658295447.jpg",
    };

}
