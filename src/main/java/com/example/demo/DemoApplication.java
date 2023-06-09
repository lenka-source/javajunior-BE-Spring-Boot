package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;

import javax.sql.DataSource;

@SpringBootApplication
@RestController
public class DemoApplication {
    // Array that I'm using to store currency data
    // private static List<MyDataObject> myDataObjects = new ArrayList<>();
    private static DataSource dataSource = new DriverManagerDataSource("jdbc:mysql://localhost:3306/javajuniorbe",
            "root",
            "tento147");

    public static MyDataObject[] fetchData() throws IOException {
        // Create a RestTemplate object
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://webapi.developers.erstegroup.com/api/csas/public/sandbox/v2/rates/exchangerates?web-api-key=c52a0682-4806-4903-828f-6cc66508329e";
        ResponseEntity<MyDataObject[]> response = restTemplate.getForEntity(url, MyDataObject[].class);
        MyDataObject[] data = response.getBody();
        return data;
    }

    public static void writeData(MyDataObject[] data) throws IOException {
        // MyDataObject[] data = fetchData();
        // Create a DataSource object to connect to the MySQL database

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        for (MyDataObject obj : data) {
            String sql = "INSERT INTO exchange_rates (shortName, validFrom, name, country, move, amount, valBuy, valSell, valMid, currBuy, currSell, currMid, version, cnbMid, ecbMid) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, obj.getShortName(), obj.getValidFrom(), obj.getName(), obj.getCountry(),
                    obj.getMove(), obj.getAmount(), obj.getValBuy(), obj.getValSell(), obj.getValMid(),
                    obj.getCurrBuy(), obj.getCurrSell(), obj.getCurrMid(), obj.getVersion(), obj.getCnbMid(),
                    obj.getEcbMid());
            // myDataObjects.add(obj); // I'm adding the items here
        }
    }

    // links: http://localhost:8080/usedb?use=false
    // http://localhost:8080/usedb?use=true

    public static void main(String[] args) throws IOException {
        SpringApplication.run(DemoApplication.class, args);
        MyDataObject[] data = fetchData(); // initial fetching and writing
        writeData(data);
    }

    @GetMapping("/usedb")
    public List<MyDataObject> useDb(@RequestParam(value = "use", defaultValue = "false") boolean usedb)
            throws IOException {
        if (usedb) {
            System.out.println("True -> I'm returning what I have in my databse.");
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            List<Map<String, Object>> data = jdbcTemplate.queryForList(
                    "SELECT shortName, validFrom, name, country, move, amount, valBuy, valSell, valMid, currBuy, currSell, currMid, version, cnbMid, ecbMid FROM exchange_rates;");
            final ObjectMapper mapper = new ObjectMapper(); // jackson's objectmapper
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            // final MyPojo pojo = mapper.convertValue(map, MyPojo.class);
            List<MyDataObject> result = new ArrayList<>();
            for (Map<String, Object> item : data) {
                result.add(mapper.convertValue(item, MyDataObject.class));
            }
            return result;
        } else {
            System.out.println("False -> I'm fetchind data from endpoint");
            // myDataObjects.clear(); // I'm clearing the list to store new data from
            // endpoint
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource); // Deleting from database
            String sql = "DELETE FROM exchange_rates";
            jdbcTemplate.update(sql);
            MyDataObject[] data = fetchData();
            writeData(data);
            return Arrays.asList(data);
        }

    }

}
