package com.devteam.aiauditserver.services.File;



import com.devteam.aiauditserver.Tools.exception.NotFoundException;
import com.devteam.aiauditserver.models.File.MediaModel;
import com.devteam.aiauditserver.repositories.File.MediaRepository;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;


@Service
public class FileServices {
    @Autowired
    private MediaRepository repository;


    private final Log logger= LogFactory.getLog(FileServices.class);


    private final ResourceLoader resourceLoader;

    public FileServices(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public InputStream getFile(String filePath) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:" + filePath);
        return resource.getInputStream();
    }

    public String readLastNLines(String filePath, int n) throws IOException {
        StringBuilder result = new StringBuilder();


        try (InputStream inputStream = resourceLoader.getResource("classpath:" + filePath).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < n) {
                result.insert(0, line + "\n"); // Insert at the beginning to maintain order
                count++;
            }
        }

        return result.toString();
    }
 
    public boolean existbyid(Long id){
        try {
            return this.repository.existsById(id);
        }catch (NoSuchElementException ex){
            throw  new NotFoundException(String.format("No file found with id [%s] in our data base",id));
        }

    }
    public void delete(MediaModel id){
        try {
             this.repository.delete(id);
        }catch (NoSuchElementException ex){
            throw  new NotFoundException(String.format("No file found with id [%s] in our data base",id));
        }
    }
    public void deleteById(Long id){
        try {
             this.repository.deleteById(id);
        }catch (NoSuchElementException ex){
            throw  new NotFoundException(String.format("No file found with id [%s] in our data base",id));
        }
    }

    public MediaModel save_file(MediaModel model){
        return this.repository.save(model);
    }
    public MediaModel update_file(MediaModel model){
        return this.repository.save(model);
    }
    public MediaModel findbyid(Long id){
        try {
            return this.repository.findById(id).get();
        }catch (NoSuchElementException ex){
            throw  new NotFoundException(String.format("No file found with id [%s] in our data base",id));
        }

    }




}
