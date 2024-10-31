package com.adobe.aem.support.contentbackflow.entity;

import java.util.List;
import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ContentSetInput implements Serializable {

    @NonNull
    public List<String> paths;
    
}
