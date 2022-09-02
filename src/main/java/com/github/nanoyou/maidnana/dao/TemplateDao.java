package com.github.nanoyou.maidnana.dao;

import com.github.nanoyou.maidnana.MaidNana;
import com.github.nanoyou.maidnana.entity.Template;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collection;

public class TemplateDao extends BaseDao<Template> {
    private final static TemplateDao instance = new TemplateDao();

    public static TemplateDao getInstance() {
        return instance;
    }

    @Override
    public Path getPath() {
        return MaidNana.INSTANCE.getDataFolderPath().resolve("templates.json");
    }

    @Override
    public Type getType() {
        return new TypeToken<Collection<Template>>(){}.getType();
    }
}
