package com.github.cloudyrock.dimmer.config.util;

import com.github.cloudyrock.dimmer.DisplayName;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import static org.powermock.api.mockito.PowerMockito.*;


@RunWith(PowerMockRunner.class)
@PrepareForTest({GitPuller.class, Git.class, FileUtils.class})
public class GitPullerUTest {

    private static final String GIT_DIR = "gitDir";
    private static final String GIT_REPO = "GIT_REPO";
    private File gitDirectory;

    private Git gitMock;

    private RebaseResult rebaseResult;

    @Before
    public void setUp() throws Exception {
        mockStatic(Git.class, FileUtils.class);
        gitDirectory = mock(File.class);
        when(gitDirectory.exists()).thenReturn(true);


        gitMock = mock(Git.class);
        PullCommand pullCommandMock = mock(PullCommand.class);
        PullResult pullResultMock = mock(PullResult.class);
        rebaseResult = mock(RebaseResult.class);
        when(Git.open(Mockito.any(File.class))).thenReturn(gitMock);
        when(gitMock.pull()).thenReturn(pullCommandMock);
        when(pullCommandMock.setRebase(Boolean.TRUE)).thenReturn(pullCommandMock);
        when(pullCommandMock.call()).thenReturn(pullResultMock);
        when(pullResultMock.getRebaseResult()).thenReturn(rebaseResult);

        doNothing().when(FileUtils.class);
        FileUtils.deleteDirectory(Mockito.any(File.class));
    }

    @Test(expected = IllegalArgumentException.class)
    @DisplayName("Should throw IllegalArgumentException when building puller if git folder is null")
    public void shouldThrowIllegalArgumentException_WhenBuildingPuller_IfGitFolderIsNull() {
        final GitPuller gitPuller = GitPuller.builder()
                .gitFolder(null)
                .gitRepository(GIT_REPO)
                .build();
    }


    @Test(expected = IllegalArgumentException.class)
    @DisplayName("Should throw IllegalArgumentException when building puller if git folder is null")
    public void shouldThrowIllegalArgumentException_WhenBuildingPuller_IfGitRepoIsNull() {
        final GitPuller gitPuller = GitPuller.builder()
                .gitFolder(new File(""))
                .gitRepository(null)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    @DisplayName("Should throw IllegalArgumentException when building puller if git folder is empty")
    public void shouldThrowIllegalArgumentException_WhenBuildingPuller_IfGitRepoIsEmpty() {
        final GitPuller gitPuller = GitPuller.builder()
                .gitFolder(new File(""))
                .gitRepository("")
                .build();
    }




    @Test
    @DisplayName("Should notify onChange consumer when git pull result is OK")
    public void shouldNotifyOnChangeConsumer_whenSubscribeOnChange_ifGitPullResultIsOK() throws InterruptedException {
        //given
        when(rebaseResult.getStatus()).thenReturn(RebaseResult.Status.OK);

        final AffectedObject objectAffected = new AffectedObject();
        final CountDownLatch latch = new CountDownLatch(1);
        final Consumer<RebaseResult> consumer = result -> {
            objectAffected.affect();
            latch.countDown();
        };

        //when
        runGitPuller(latch, consumer, null, null);

        //then
        Assert.assertTrue(objectAffected.isAffected());
    }

    @Test
    @DisplayName("Should notify onChange consumer when git pull result is FAST_FORWARD")
    public void shouldNotifyOnChangeConsumer_whenSubscribeOnChange_ifGitPullResultIsFAST_FORWARD() throws InterruptedException {
        //given
        when(rebaseResult.getStatus()).thenReturn(RebaseResult.Status.FAST_FORWARD);

        final AffectedObject objectAffected = new AffectedObject();
        final CountDownLatch latch = new CountDownLatch(1);
        final Consumer<RebaseResult> consumer = result -> {
            objectAffected.affect();
            latch.countDown();
        };

        //when
        runGitPuller(latch, consumer, null, null);

        //then
        Assert.assertTrue(objectAffected.isAffected());
    }

    @Test
    @DisplayName("Should not notify onChange consumer when git pull result is NOT FAST_FORWARD or OK")
    public void shouldNotNotifyOnChangeConsumer_whenSubscribeOnChange_ifGitPullResultIsNotFAST_FORWARDOrOK() throws InterruptedException {
        //given
        when(rebaseResult.getStatus()).thenReturn(RebaseResult.Status.UP_TO_DATE);

        final AffectedObject changeAffected = new AffectedObject();
        final CountDownLatch latch = new CountDownLatch(1);
        final Consumer<RebaseResult> onChangeConsumer = result -> changeAffected.affect();
        final Consumer<RebaseResult> onAnyConsumer = result -> latch.countDown();

        //when
        runGitPuller(latch, onChangeConsumer, onAnyConsumer, null);

        //then
        Assert.assertFalse(changeAffected.isAffected());
    }

    @Test
    @DisplayName("Should notify onAny consumer when git pull result is OK")
    public void shouldNotifyOnAnyConsumer_whenSubscribeOnAny_ifGitPullResultIsOK() throws InterruptedException {
        //given
        when(rebaseResult.getStatus()).thenReturn(RebaseResult.Status.OK);

        final AffectedObject objectAffected = new AffectedObject();
        final CountDownLatch latch = new CountDownLatch(1);
        final Consumer<RebaseResult> consumer = result -> {
            objectAffected.affect();
            latch.countDown();
        };

        //when
        runGitPuller(latch, null, consumer, null);

        //then
        Assert.assertTrue(objectAffected.isAffected());
    }


    @Test
    @DisplayName("Should notify onAny consumer when git pull result is FAST_FORWARD")
    public void shouldNotifyOnAnyConsumer_whenSubscribeOnAny_ifGitPullResultIsFAST_FORWARD() throws InterruptedException {
        //given
        when(rebaseResult.getStatus()).thenReturn(RebaseResult.Status.FAST_FORWARD);

        final AffectedObject objectAffected = new AffectedObject();
        final CountDownLatch latch = new CountDownLatch(1);
        final Consumer<RebaseResult> consumer = result -> {
            objectAffected.affect();
            latch.countDown();
        };

        //when
        runGitPuller(latch, null, consumer, null);

        //then
        Assert.assertTrue(objectAffected.isAffected());
    }

    @Test
    @DisplayName("Should notify onAny consumer when git pull result is NOT FAST_FORWARD or OK")
    public void shouldNotifyOnAnyConsumer_whenSubscribeOnChange_ifGitPullResultIsNotFAST_FORWARDOrOK() throws InterruptedException {
        //given
        when(rebaseResult.getStatus()).thenReturn(RebaseResult.Status.UP_TO_DATE);

        final AffectedObject changeAffected = new AffectedObject();
        final CountDownLatch latch = new CountDownLatch(1);
        final Consumer<RebaseResult> onChangeConsumer = result -> changeAffected.affect();
        final Consumer<RebaseResult> onAnyConsumer = result -> latch.countDown();

        //when
        runGitPuller(latch, onChangeConsumer, onAnyConsumer, null);

        //then
        Assert.assertFalse(changeAffected.isAffected());
    }


    @Test
    @DisplayName("Should reset folder when pulling if no successful result")
    public void shouldResetFolder_whenPulling_ifNoSuccessfulResult() throws InterruptedException, IOException {
        //given
        when(rebaseResult.getStatus())
                .thenReturn(RebaseResult.Status.CONFLICTS)
                .thenReturn(RebaseResult.Status.OK);

        final AffectedObject changeAffected = new AffectedObject();
        final CountDownLatch latch = new CountDownLatch(1);
        final Consumer<RebaseResult> onChangeConsumer = result -> {
            changeAffected.affect();
            latch.countDown();
        };

        //when
        runGitPuller(latch, onChangeConsumer, null, null, 10L, 75L);

        //then
        Assert.assertTrue(changeAffected.isAffected());

        verifyStatic(FileUtils.class);
        FileUtils.deleteDirectory(gitDirectory);

    }

    public void runGitPuller(CountDownLatch latch,
                             Consumer<RebaseResult> onChangeConsumer,
                             Consumer<RebaseResult> onAnyConsumer,
                             Consumer<Throwable> onErrorConsumer) throws InterruptedException {
        runGitPuller(latch, onChangeConsumer, onAnyConsumer, onErrorConsumer, 10L, 1000L * 60 * 60);
    }

    public void runGitPuller(CountDownLatch latch,
                             Consumer<RebaseResult> onChangeConsumer,
                             Consumer<RebaseResult> onAnyConsumer,
                             Consumer<Throwable> onErrorConsumer,
                             long initialDelay,
                             long period) throws InterruptedException {

        //when
        final GitPuller gitPuller = GitPuller.builder()
                .gitFolder(gitDirectory)
                .gitRepository(GIT_REPO)
                .initialDelayMilliSeconds(initialDelay)
                .periodMilliseconds(period)
                .build();

        final ScheduledFuture<?> scheduler = gitPuller.subscribe(onAnyConsumer, onChangeConsumer, onErrorConsumer);
        latch.await();

        //tearDown
        scheduler.cancel(true);
    }

    private static class AffectedObject {
        private boolean affected = false;

        void affect() {
            affected = true;
        }

        boolean isAffected() {
            return affected;
        }
    }

}