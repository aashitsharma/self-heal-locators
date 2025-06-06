package onemg.analytics.dump.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.Map;


public class CommonUtility {

    private static final Logger LOGGER = Logger.getLogger(CommonUtility.class);

    public static Map<String, Object> readJsonAsMap(String filePath) {
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream is = CommonUtility.class.getClassLoader().getResourceAsStream(filePath)) {
            if (is == null) {
                throw new IllegalArgumentException("File not found: " + filePath);
            }
            return mapper.readValue(is, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON file", e);
        }
    }

}
