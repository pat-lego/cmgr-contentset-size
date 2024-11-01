package com.adobe.aem.support.contentbackflow.entity;

import java.util.Calendar;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@NoArgsConstructor @Getter @Setter
public class ContentSetResult {
    
    @NonNull
    public List<String> paths;

    @NonNull
    public String status;

    public long total;

    @NonNull
    public Calendar completed;
}
