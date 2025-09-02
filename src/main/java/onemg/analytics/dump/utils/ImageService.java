package onemg.analytics.dump.utils;

import com.mongodb.client.gridfs.model.GridFSFile;

import onemg.analytics.dump.model.ImageDataModel;
import onemg.analytics.dump.repository.ImageDataRepository;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Service
public class ImageService {

    private final GridFsTemplate gridFsTemplate;

    @Autowired
    public ImageService(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }
    @Autowired
    ImageDataRepository imageRepo;

    // Save image to GridFS
    public ImageDataModel saveImage(MultipartFile file) throws IOException {
        ObjectId id = gridFsTemplate.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType()
        );
        byte[] imageBytes = StreamUtils.copyToByteArray(file.getInputStream());
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        ImageDataModel dataModel = new ImageDataModel();
        dataModel.setImageHexId(id.toHexString());
        dataModel.setBase64(base64);
        imageRepo.save(dataModel);
        return dataModel;
    }

    // Get file metadata
    public GridFSFile getFileById(String id) {
        return gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(id))));
    }

    // Get file as resource (for streaming back to client)
    public GridFsResource getFileResource(GridFSFile file) {
        return gridFsTemplate.getResource(file);
    }
}