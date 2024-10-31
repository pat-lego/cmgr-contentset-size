package com.adobe.aem.support.contentbackflow.entity;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class ContentSetJob {

    @NonNull
    public String jobId;

    @NonNull
    public String status;

    @NonNull
    public ContentSetInput input;

}
