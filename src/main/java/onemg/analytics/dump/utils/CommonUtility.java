package onemg.analytics.dump.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import onemg.analytics.dump.model.BaseModel;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;


public class CommonUtility {

    private static final Logger LOGGER = Logger.getLogger(CommonUtility.class);

    @Autowired
    private static ObjectMapper objectMapper;

    /**
     * Method is responsible for parsing json from file path provided and return Map
     * @param filePath : Provide Path from source route (path after src/main/resources/)
     * @return Map<String, Object> which can be used to return
     */
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

    /**
     *
     * @param enumeration : Required to extract headers
     * @return
     */
    public static String extractHeaders(Enumeration<String> enumeration, HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        while (enumeration.hasMoreElements()) {
            String key = enumeration.nextElement();
            sb.append(key +" : "+request.getHeader(key));
            if (enumeration.hasMoreElements()) {
                sb.append(", ");
            }
        }
        sb.append(" }");
        return sb.toString();
    }
    @SuppressWarnings("unchecked")
    public static Map<String, Object> convertDtoToMap(BaseModel dto) {
        objectMapper = new ObjectMapper();
        return objectMapper.convertValue(dto, Map.class);

    }

}
