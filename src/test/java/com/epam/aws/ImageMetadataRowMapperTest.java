package com.epam.aws;

import com.epam.aws.model.ImageMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ImageMetadataRowMapperTest {

    private ImageMetadataRowMapper rowMapper;

    @BeforeEach
    void setUp() {
        rowMapper = new ImageMetadataRowMapper();
    }

    @Test
    void testMapRow() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        long id = 1L;
        String fileName = "image.jpg";
        long fileSize = 1024L;
        String fileExtension = "jpg";
        Date lastUpdateDate = new Date();

        when(rs.getLong("id")).thenReturn(id);
        when(rs.getString("file_name")).thenReturn(fileName);
        when(rs.getLong("file_size")).thenReturn(fileSize);
        when(rs.getString("file_extension")).thenReturn(fileExtension);
        when(rs.getTimestamp("last_update_date")).thenReturn(new Timestamp(lastUpdateDate.getTime()));

        // Call mapRow
        ImageMetadata metadata = rowMapper.mapRow(rs, 1);

        // Assert the values
        assertEquals(id, metadata.getId());
        assertEquals(fileName, metadata.getFileName());
        assertEquals(fileSize, metadata.getFileSize());
        assertEquals(fileExtension, metadata.getFileExtension());
        assertEquals(lastUpdateDate, metadata.getLastUpdateDate());

        // Verify interactions with the ResultSet
        verify(rs, times(1)).getLong("id");
        verify(rs, times(1)).getString("file_name");
        verify(rs, times(1)).getLong("file_size");
        verify(rs, times(1)).getString("file_extension");
        verify(rs, times(1)).getTimestamp("last_update_date");
    }
}
