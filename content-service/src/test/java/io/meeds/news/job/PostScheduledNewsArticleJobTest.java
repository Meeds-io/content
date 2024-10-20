package io.meeds.news.job;

import static io.meeds.news.service.impl.NewsServiceImpl.SCHEDULE_POST_DATE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;

import io.meeds.news.model.News;
import io.meeds.news.service.NewsService;

@RunWith(MockitoJUnitRunner.class)
public class PostScheduledNewsArticleJobTest {

  @Mock
  private MetadataService    metadataService;

  @Mock
  private NewsService        newsService;

  @InjectMocks
  private PostScheduledNewsArticleJob postScheduledNewsArticleJob;

  @BeforeEach
  public void setUp() {
    // initializes all fields annotated with @Mock within the test class
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testPostScheduledNewsArticle() throws Exception {
    String newsId = "1";
    LocalDateTime currentDate = LocalDateTime.now(ZoneId.of("Africa/Tunis")).truncatedTo(ChronoUnit.MINUTES);

    MetadataItem metadataItemWithDateBeforeTheCurrentDate = mock(MetadataItem.class);
    when(metadataItemWithDateBeforeTheCurrentDate.getObjectId()).thenReturn(newsId);
    LocalDateTime beforeDate = currentDate.minusDays(1);
    String beforeDateString = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss").format(beforeDate).concat(" ").concat("+0100");

    SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss" + "Z");
    Calendar startPublishedDate = Calendar.getInstance();
    startPublishedDate.setTime(format.parse(beforeDateString));

    SimpleDateFormat defaultFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    defaultFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
    String startPublishedDateString = defaultFormat.format(startPublishedDate.getTime());

    when(metadataItemWithDateBeforeTheCurrentDate.getProperties()).thenReturn(Map.of(SCHEDULE_POST_DATE, startPublishedDateString));

    MetadataItem metadataItemWithDateAfterTheCurrentDate = mock(MetadataItem.class);
    LocalDateTime afterDate = currentDate.plusDays(1);
    String afterDateString = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss").format(afterDate).concat(" ").concat("+0100");
    // after date
    startPublishedDate.setTime(format.parse(afterDateString));
    String afterSatrtDateString = defaultFormat.format(startPublishedDate.getTime());
    when(metadataItemWithDateAfterTheCurrentDate.getProperties()).thenReturn(Map.of(SCHEDULE_POST_DATE, afterSatrtDateString));

    List<MetadataItem> metadataItems = new ArrayList<>();
    metadataItems.add(metadataItemWithDateBeforeTheCurrentDate);
    metadataItems.add(metadataItemWithDateAfterTheCurrentDate);

    when(metadataService.getMetadataItemsByMetadataNameAndTypeAndObjectAndMetadataItemProperty(anyString(),
                                                                                               anyString(),
                                                                                               anyString(),
                                                                                               anyString(),
                                                                                               anyString(),
                                                                                               anyLong(),
                                                                                               anyLong())).thenReturn(metadataItems);

    News news = mock(News.class);
    when(news.getAuthor()).thenReturn("author");
    when(newsService.getNewsArticleById(newsId)).thenReturn(news);

    // When
    postScheduledNewsArticleJob.postScheduledNewsArticle();

    // Then
    // Assert that the post news method called only once
    verify(newsService, times(1)).postNews(news, news.getAuthor());
  }
}
