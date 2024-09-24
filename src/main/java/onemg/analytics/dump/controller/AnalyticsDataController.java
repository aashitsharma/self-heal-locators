package onemg.analytics.dump.controller;

import onemg.analytics.dump.model.model.DynamicData;
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
    public ResponseEntity<DynamicData> createEventsData(@RequestBody Map<String, Object> data) {
        try {
            DynamicData dynamicData = new DynamicData(data);
            Optional<DynamicData> dataDump = dynamicDataRepository.findByDataName((String) data.get("name"));
            LOGGER.info("/event/create Name of Event : "+ data.get("name") + " - Ref Id:"+ Thread.currentThread().getId());
            if (!dataDump.isPresent()){
                LOGGER.info("/event/create saving Data for : "+ data.get("name")+ " - Ref Id:"+ Thread.currentThread().getId());
                DynamicData savedData = dynamicDataRepository.save(dynamicData);
                return new ResponseEntity<>(savedData, HttpStatus.CREATED);
            }
            else {
                LOGGER.info("/event/create Event already exist for : "+ data.get("name")+ " - Ref Id:"+ Thread.currentThread().getId());
                return new ResponseEntity(dataDump,HttpStatus.OK);
            }

        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET API to fetch data by name
    @GetMapping("event/{name}")
    public ResponseEntity<Object> getDataByName(@PathVariable("name") String name) {
        Optional<DynamicData> dataDump = dynamicDataRepository.findByDataName(name);

        if (dataDump.isPresent()) {
            return new ResponseEntity<>(dataDump, HttpStatus.OK);  // Return the 'data' field
        } else {
            return new ResponseEntity<>("Data not found", HttpStatus.NOT_FOUND);
        }
    }

}
