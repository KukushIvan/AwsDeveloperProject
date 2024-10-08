package com.epam.aws;

import com.epam.aws.model.ImageMetadata;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class ImageMetadataRowMapper implements RowMapper<ImageMetadata> {
    @Override
    public ImageMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
        ImageMetadata metadata = new ImageMetadata();
        metadata.setId(rs.getLong("id"));
        metadata.setFileName(rs.getString("file_name"));
        metadata.setFileSize(rs.getLong("file_size"));
        metadata.setFileExtension(rs.getString("file_extension"));
        metadata.setLastUpdateDate(new Date(rs.getTimestamp("last_update_date").getTime()));
        return metadata;
    }
}