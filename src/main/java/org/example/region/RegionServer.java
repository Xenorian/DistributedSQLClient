package org.example.region;

import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.example.Raft.Server;
import org.example.api.MasterClientService;
import org.example.api.MasterRegionService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class RegionServer {
    static String ZOOKEEPER_ADDRESS="";
    static String Application_Name="region-service-caller";
    public void run() {
        // For both server and client
        String raftGroupId;
        String peers;
        // For server only
        String id;
        File storageDir;

        Properties props = new Properties();
        try {
            // 从文件中读取配置信息
            FileInputStream fis = new FileInputStream("config.properties");
            props.load(fis);
            fis.close();

            // 获取属性值
            raftGroupId = props.getProperty("raftGroupId");
            peers = props.getProperty("peers");
            id = props.getProperty("id");
            storageDir = new File(props.getProperty("storageDir"));

            // 获取属性值
            String ZOOKEEPER_HOST = props.getProperty("zookeeper.address");
            String ZOOKEEPER_PORT = props.getProperty("zookeeper.port");
            ZOOKEEPER_ADDRESS = "zookeeper://" + ZOOKEEPER_HOST + ":" + ZOOKEEPER_PORT;

            // 输出属性值
            System.out.println("raftGroupId: " + raftGroupId);
            System.out.println("peers: " + peers);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        ReferenceConfig<MasterRegionService> reference = new ReferenceConfig<>();
        reference.setInterface(MasterRegionService.class);


        DubboBootstrap.getInstance()
                .application(Application_Name)
                .registry(new RegistryConfig(ZOOKEEPER_ADDRESS))
                .start()
                .reference(reference);

        MasterRegionService service = reference.get();
        service.ReportRegion(raftGroupId,peers);

        Server core = new Server(raftGroupId,peers,id,storageDir);
//        Client test = new Client(raftGroupId,peers);

        service.ReportRegion(raftGroupId,peers);

        try {
            core.run();
//            test.run();
//            test.operation("select * from devices");
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
