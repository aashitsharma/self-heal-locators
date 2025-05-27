package onemg.analytics.dump.controller;

import onemg.analytics.dump.model.DynamicData;
import onemg.analytics.dump.model.ErrorModel;
import onemg.analytics.dump.repository.DynamicDataRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/api")
public class AnalyticsDataController {

    private static final Logger LOGGER = Logger.getLogger(AnalyticsDataController.class);

    @Autowired
    private DynamicDataRepository dynamicDataRepository;

    @PostMapping
    @RequestMapping("event/create")
    public ResponseEntity<Object> createEventsData(@RequestBody Map<String, Object> data) {
        try {
            DynamicData dynamicData = new DynamicData(data);
            Optional<DynamicData> dataDump = dynamicDataRepository.findByDataName((String) data.get("event_name"));
            LOGGER.info("/event/create Name of Event : "+ data.get("name"));
            if (!dataDump.isPresent()){
                LOGGER.info("/event/create saving Data for : "+ data.get("event_name"));
                DynamicData savedData = dynamicDataRepository.save(dynamicData);

                return new ResponseEntity<>(savedData, HttpStatus.CREATED);
            }
            else {
                LOGGER.info("/event/create Event already exist for : "+ data.get("event_name"));
                return new ResponseEntity(dataDump,HttpStatus.OK);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new ErrorModel().errorResp(HttpStatus.INTERNAL_SERVER_ERROR.value(),e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET API to fetch data by name
    @GetMapping("event/{event_name}")
    public ResponseEntity<Object> getDataByName(@PathVariable("event_name") String name) {
        Optional<DynamicData> dataDump = dynamicDataRepository.findByDataName(name);

        if (dataDump.isPresent()) {
            LOGGER.info("Fetching Events for : "+name);
            return new ResponseEntity<>(dataDump, HttpStatus.OK);  // Return the 'data' field
        } else {
            return new ResponseEntity<>("Data not found", HttpStatus.NOT_FOUND);
        }
    }

}
