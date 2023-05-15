package org.example.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

enum SqlType {
    SELECT,
    UPDATE,
    INSERT,
    DELETE,
    CREATE,
    DROP,
    INVALID
}
public class TableRouter {
    private ConcurrentHashMap<String, String> TableRegion;
    private ConcurrentHashMap<String, List<String>> Regions;
    private ConcurrentHashMap<String, Set<String>> RegionTables;
    public SqlType getTypeOfSql(String sql) {
        String[] tokens = sql.split(" ");
        SqlType type = SqlType.INVALID;
        switch (tokens[0].toLowerCase()) {
            case "create": type = SqlType.CREATE; break;
            case "insert": type = SqlType.INSERT; break;
            case "update": type = SqlType.UPDATE; break;
            case "delete": type = SqlType.DELETE; break;
            case "select": type = SqlType.SELECT; break;
            case "drop":   type = SqlType.DROP;   break;
        }
        return type;
    }

    public String getTableOfQuerySql(String sql) {
        String[] tokens = sql.split(" ");
        for(int i = 0; i < tokens.length; i++) {
            if(tokens[i].toLowerCase().equals("from") && i + 1 < tokens.length)
                return tokens[i+1];
        }
        return "";
    }

    public String getTableOfControlSql(String sql) {
        String[] tokens = sql.split(" ");
        for(int i = 0; i < tokens.length; i++) {
            if(tokens[i].toLowerCase().equals("table") && i + 1 < tokens.length)
                return tokens[i+1];
        }
        return "";
    }

    public void regionAddServer(Integer regionId, String serverIp) {
        if(Regions.containsKey(regionId)) {
            Regions.get(regionId).add(serverIp);
        }
    }

    public void addRegion(String regionId, List<String> servers) {
        if(Regions.containsKey(regionId))
            return;
        Regions.put(regionId, servers);
        RegionTables.put(regionId, new HashSet<>());
    }

    public List<String> getServersOfTable(String table) {
        if(!TableRegion.containsKey(table))
            return null;
        String RegionId = TableRegion.get(table);
        return Regions.get(RegionId);
    }

    String ControlTable(String table, SqlType type) {
        if(type.equals(SqlType.DROP)) {
            String region = TableRegion.get(table);
            if(region != null) {
                RegionTables.get(region).remove(table);
                TableRegion.remove(table);
                return region;
            }
        } else if (type.equals(SqlType.CREATE)) {
            String regionId = null;
            Integer minSize = Integer.MAX_VALUE;
            for(Map.Entry<String, Set<String>> entry : RegionTables.entrySet()) {
                if(entry.getValue().size() < minSize) {
                    minSize = entry.getValue().size();
                    regionId = entry.getKey();
                }
            }
            if(regionId != null) {
                RegionTables.get(regionId).add(table);
                TableRegion.put(table, regionId);
                return regionId;
            }
        }
        return null;
    }

    // return : region id for the table
    public String getRegionAndExecute(String sql) {
        SqlType type = getTypeOfSql(sql);
        String table = "";
        if(type.equals(SqlType.CREATE) || type.equals(SqlType.DROP)) {
            table = getTableOfControlSql(sql);
            if(table.equals(""))
                return "";
            return ControlTable(table, type);
        }
        else if(type.equals(SqlType.INVALID)) {
            return "";
        }
        else {
            table = getTableOfQuerySql(sql);
            if (table.equals(""))
                return "";
            return TableRegion.get(table);
        }
    }

    public String getRegionPeers(String region) {
        List<String> Servers = Regions.get(region);
        if(Servers == null)
            return "";
        String res = "";
        for (int i = 0; i < Servers.size(); i++) {
            if (i > 0) {
                res += ",";
            }
            res += Integer.toString(i);
            res += ":";
            res += Servers.get(i);
        }
        return res;
    }

    public TableRouter() {
        TableRegion = new ConcurrentHashMap<>();
        Regions = new ConcurrentHashMap<>();
        RegionTables = new ConcurrentHashMap<>();

        // todo : use files for initialization and persistence.
        List<String> region1 = new ArrayList<>();
        region1.add("127.0.0.1:8080");
        region1.add("127.0.0.1:8081");
        region1.add("127.0.0.1:8082");

        List<String> region2 = new ArrayList<>();
        region2.add("127.0.0.1:8083");
        region2.add("127.0.0.1:8084");
        region2.add("127.0.0.1:8085");

        Regions.put("region1", region1);
        Regions.put("region2", region2);
        RegionTables.put("region1", new HashSet<>());
        RegionTables.put("region2", new HashSet<>());
    }
}

