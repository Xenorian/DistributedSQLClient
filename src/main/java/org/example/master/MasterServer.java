package org.example.master;

import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;

import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;


import org.example.api.*;
import org.example.util.TableRouter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import java.util.ServiceConfigurationError;

public class MasterServer {

    public void run() {

        Properties props = new Properties();
        String ZOOKEEPER_HOST = "", ZOOKEEPER_PORT = "", ZOOKEEPER_ADDRESS = "";
        try {
            // 从文件中读取配置信息
            FileInputStream fis = new FileInputStream("config.properties");
            props.load(fis);
            fis.close();

            // 获取属性值
            ZOOKEEPER_HOST = props.getProperty("zookeeper.address");
            ZOOKEEPER_PORT = props.getProperty("zookeeper.port");
            ZOOKEEPER_ADDRESS = "zookeeper://" + ZOOKEEPER_HOST + ":" + ZOOKEEPER_PORT;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        System.out.println(ZOOKEEPER_ADDRESS);

        ServiceConfig<MasterClientService> clientService = new ServiceConfig<>();
        clientService.setInterface(MasterClientService.class);
        TableRouter TheRouter = new TableRouter("RegionStoreFile", "TableStoreFile");
        clientService.setRef(new MasterClientServiceImpl(TheRouter));
        ServiceConfig<MasterRegionService> regionService = new ServiceConfig<>();
        regionService.setInterface(MasterRegionService.class);
        regionService.setRef(new MasterRegionServiceImpl(TheRouter));
        DubboBootstrap.getInstance()
                .application("master-service")
                .registry(new RegistryConfig(ZOOKEEPER_ADDRESS))
                .protocol(new ProtocolConfig("dubbo", -1))
                .service(clientService)
                .service(regionService)
                .start()
                .await();
    }
}
