package com.simple.log.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

import lombok.Data;

/**
 * @author fdrama
 * @date 2022年08月23日 14:34
 */
@Data
@Document("user")
public class User implements Serializable {

    private static final long serialVersionUID = -523462011851591068L;

    @Id
    private String id;
    private String name;
}
