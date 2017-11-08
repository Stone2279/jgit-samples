package de.stone.jgit.samples;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.CherryPickResult.CherryPickStatus;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JGitTest {

	private File jgitDir = new File("jgit");
	private File existingDir = new File("existing");

	@Before
	public void init() throws Exception {

		// create a new git repository. Missing directories are created
		// automatically.
		Git git = Git.init().setDirectory(jgitDir).call();
		assertNotNull(git);
		assertTrue(git.status().call().isClean());
	}

	@After
	public void cleanup() throws Exception {

		FileUtils.deleteDirectory(jgitDir);

		if (existingDir.exists())
			FileUtils.deleteDirectory(existingDir);
	}

	@Test
	public void callInitAgain() throws Exception {

		// Call the init command on an already initialized git repository does
		// nothing and just returns an instance of the already existing
		// repository
		Git git = Git.init().setDirectory(jgitDir).call();
		assertNotNull(git);
	}

	@Test
	public void initExistingDirectory() throws Exception {

		// create an new folder and copy some test files to it
		Git git = initExisting();

		assertNotNull(git);
		assertFalse(git.getRepository().isBare());
		assertFalse(git.status().call().isClean());

		// check if the new repository has a HEAD revision
		Ref headRef = git.getRepository().findRef(Constants.HEAD);
		assertNotNull(headRef);

		// check if we find the copied files with status unchecked
		Set<String> untracked = git.status().call().getUntracked();
		// untracked.forEach(u -> System.out.println(u));
		assertEquals(5, untracked.size());
	}

	@Test
	public void addAndCommitOnExistingDirectory() throws Exception {

		Git git = initExisting();

		// add the existing files to git
		git.add().addFilepattern(".").call();
		Set<String> untracked = git.status().call().getUntracked();
		// untracked.forEach(u -> System.out.println(u));
		assertEquals(0, untracked.size());

		// do the inital commit
		RevCommit revCommit = git.commit().setMessage("initial commit").call();
		assertEquals("initial commit", revCommit.getFullMessage());
	}

	@Test
	public void removeAndCommitOnExistingDirectory() throws Exception {

		Git git = initialSetup();

		// delete some files
		new File(existingDir, "test1.txt").delete();
		new File(existingDir, "test2.txt").delete();

		// for rm no wildcards can be used?
		git.rm().addFilepattern("test1.txt").call();
		git.rm().addFilepattern("test2.txt").call();

		Set<String> removed = git.status().call().getRemoved();
		assertEquals(2, removed.size());

		// comit the deleted files
		RevCommit revCommit = git.commit().setMessage("removed some files").call();
		assertEquals("removed some files", revCommit.getFullMessage());
	}

	@Test
	public void createNewBranch() throws Exception {

		Git git = initialSetup();

		// list all existing branches
		List<Ref> branches = git.branchList().call();
		assertEquals(1, branches.size());
		assertEquals("refs/heads/master", branches.get(0).getName());

		// create a new branch
		git.branchCreate().setName("my-branch").call();
		branches = git.branchList().call();
		assertEquals(2, branches.size());
		assertEquals("refs/heads/master", branches.get(0).getName());
		assertEquals("refs/heads/my-branch", branches.get(1).getName());

		// check if we still use master branch
		assertEquals("master", git.getRepository().getBranch());

		// checkout the new branch
		git.checkout().setName("my-branch").call();
		assertEquals("my-branch", git.getRepository().getBranch());
	}

	@Test
	public void createTag() throws Exception {

		Git git = initialSetup();

		// there should be no tags
		List<Ref> tags = git.tagList().call();
		assertEquals(0, tags.size());

		// create a tag
		git.tag().setName("my-tag").setMessage("tag of my first release").call();

		// checks for the newly created tag
		tags = git.tagList().call();
		assertEquals(1, tags.size());
		assertEquals("refs/tags/my-tag", tags.get(0).getName());
	}

	@Test
	public void createBranchFromTag() throws Exception {

		Git git = initialSetup();
		
		// create a tag
		git.tag().setName("my-tag").setMessage("tag of my first release").call();
		// create a new branch from the tag and checkout the new branch
		git.checkout().setName("my-tag").setCreateBranch(true).setName("bugfixing").call();
		
		List<Ref> branches = git.branchList().call();
		assertTrue(branches.stream()
			.map(b -> b.getName())
			.collect(Collectors.toList())
			.contains("refs/heads/bugfixing"));
		
		assertEquals("bugfixing", git.getRepository().getBranch());
	}
	
	@Test
	public void listBranches() throws Exception {
		
		Git git = initialSetup();
		
		// create some branches
		git.branchCreate().setName("first-branch").call();
		git.branchCreate().setName("second-branch").call();
		git.branchCreate().setName("third-branch").call();
		
		// list branches
		List<Ref> branches = git.branchList().call();
		assertEquals(4, branches.size());
		
		// get branch names as strings
		List<String> branchNames = branches.stream()
			.map(b -> b.getName())
			.collect(Collectors.toList());
		
		assertTrue(branchNames.contains("refs/heads/master"));
		assertTrue(branchNames.contains("refs/heads/third-branch"));
		assertTrue(branchNames.contains("refs/heads/first-branch"));
		assertTrue(branchNames.contains("refs/heads/second-branch"));
	}
	
	@Test
	public void listTags() throws Exception {
		
		Git git = initialSetup();
		
		// create a tag
		git.tag().setName("first-tag").setMessage("tag of my first release").call();
		git.tag().setName("second-tag").setMessage("tag of my second release").call();
		git.tag().setName("third-tag").setMessage("tag of my third release").call();
		
		// list tags
		List<Ref> tags = git.tagList().call();
		assertEquals(3, tags.size());
		
		// get tag names as strings
		List<String> tagNames = tags.stream()
			.map(b -> b.getName())
			.collect(Collectors.toList());
		
		assertTrue(tagNames.contains("refs/tags/first-tag"));
		assertTrue(tagNames.contains("refs/tags/second-tag"));
		assertTrue(tagNames.contains("refs/tags/third-tag"));
	}
	
	@Test
	public void getCommitHistory() throws Exception {
		
		Git git = initialSetup();
		
		// first commit
		editFile(new File(existingDir, "test1.txt"));
		editFile(new File(existingDir, "test2.txt"));
		
		git.add().addFilepattern(".").call();
		git.commit().setMessage("First changes").call();
		
		// second commit
		editFile(new File(existingDir, "test3.txt"));
		
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Second changes").call();
		
		// third commit
		editFile(new File(new File(existingDir, "subdir"), "sub1.txt"));
		
		git.add().addFilepattern(".").call();
		git.commit().setMessage("Third changes").call();
		
		Iterable<RevCommit> logs = git.log().add(git.getRepository().resolve("master")).setMaxCount(5).call();
		List<LogEntry> logEntries = StreamSupport.stream(logs.spliterator(), false)
			.map(l -> new LogEntry(l))
			.collect(Collectors.toList());
		
		assertEquals(4, logEntries.size());
//		logEntries.forEach(l -> System.out.println(l));
	}
	
	@Test
	public void diff() throws Exception {
		
		Git git = initialSetup();
		
		// do some changes and commit
		new File(existingDir, "test2.txt").delete();
		git.rm().addFilepattern("test2.txt").call();
		
		editFile(new File(existingDir, "test1.txt"));
		git.add().addFilepattern(".").call();
		
		RevCommit revCommit = git.commit().setMessage("First changes").call();
		
		// do a diff to the previous version
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();  DiffFormatter diffFormatter = new DiffFormatter(bos)) {
	        diffFormatter.setRepository(git.getRepository());
	        for (DiffEntry entry : diffFormatter.scan(revCommit.getParent(0), revCommit)) {
	            diffFormatter.format(diffFormatter.toFileHeader(entry));
	        }
	        
	        System.out.println(bos.toString());
	    }
		
		// TODO: check jgit documentation for more info about diffs
		
	}
	
	@Test
	public void mergeFastForward() throws Exception
	{
		Git git = initialSetup();
		
		// create new branch and checkout
		git.checkout().setCreateBranch(true).setName("my-branch").call();
		
		// do some changes and commit
		editFile(new File(existingDir, "test1.txt"));
		editFile(new File(existingDir, "test2.txt"));
		
		git.add().addFilepattern(".").call();
		git.commit().setMessage("some changes").call();
		
		// merge
		git.checkout().setName("master").call();
		MergeResult mergeResult = git.merge().include(git.getRepository().findRef("my-branch")).call();
		
		assertTrue(mergeResult.getMergeStatus().isSuccessful());
		assertEquals(MergeStatus.FAST_FORWARD, mergeResult.getMergeStatus());
	}
	
	@Test
	public void mergeWithAutoCommit() throws Exception
	{
		Git git = initialSetup();
				
		// create new branch and checkout
		git.checkout().setCreateBranch(true).setName("my-branch").call();
		
		// do some changes and commit
		editFile(new File(existingDir, "test1.txt"));
		editFile(new File(existingDir, "test2.txt"));
		
		git.add().addFilepattern(".").call();
		git.commit().setMessage("some changes on branch").call();
		
		// back to master
		git.checkout().setName("master").call();
		
		// do some changes on master and commit
		editFile(new File(existingDir, "test3.txt"));
		git.add().addFilepattern(".").call();
		git.commit().setMessage("some changes on master").call();
		
		// merge
		MergeResult mergeResult = git.merge().include(git.getRepository().findRef("my-branch")).call();
		
		assertTrue(mergeResult.getMergeStatus().isSuccessful());
		assertEquals(MergeStatus.MERGED, mergeResult.getMergeStatus());
	}
	
	@Test
	public void mergeWithoutAutoCommit() throws Exception
	{
		Git git = initialSetup();
				
		// create new branch and checkout
		git.checkout().setCreateBranch(true).setName("my-branch").call();
		
		// do some changes and commit
		editFile(new File(existingDir, "test1.txt"));
		editFile(new File(existingDir, "test2.txt"));
		
		git.add().addFilepattern(".").call();
		git.commit().setMessage("some changes on branch").call();
		
		// back to master
		git.checkout().setName("master").call();
		
		// do some changes on master and commit
		editFile(new File(existingDir, "test3.txt"));
		git.add().addFilepattern(".").call();
		git.commit().setMessage("some changes on master").call();
		
		// merge
		MergeResult mergeResult = git.merge().setCommit(false)
				.include(git.getRepository().findRef("my-branch")).call();
		
		assertTrue(mergeResult.getMergeStatus().isSuccessful());
		assertEquals(MergeStatus.MERGED_NOT_COMMITTED, mergeResult.getMergeStatus());
	}
	
	@Test
	public void mergeWithConflicts() throws Exception
	{
		Git git = initialSetup();
		
		// create new branch and checkout
		git.checkout().setCreateBranch(true).setName("my-branch").call();
		
		// do some changes and commit
		editFile(new File(existingDir, "test1.txt"));
		editFile(new File(existingDir, "test2.txt"));
		
		git.add().addFilepattern(".").call();
		git.commit().setMessage("some changes on branch").call();
		
		// back to master
		git.checkout().setName("master").call();
		
		// do some changes on master and commit
		editFile(new File(existingDir, "test1.txt"));
		editFile(new File(existingDir, "test2.txt"));
		git.add().addFilepattern(".").call();
		git.commit().setMessage("some changes on master").call();
		
		// merge
		MergeResult mergeResult = git.merge().include(git.getRepository().findRef("my-branch")).call();
		
		assertFalse(mergeResult.getMergeStatus().isSuccessful());
		assertEquals(MergeStatus.CONFLICTING, mergeResult.getMergeStatus());
		assertEquals(2, mergeResult.getConflicts().size());
		
		// undo the merge
		git.reset().setMode(ResetType.HARD).call();
		Status status = git.status().call();
		assertTrue(status.isClean());
	}
	
	@Test
	public void cherryPick() throws Exception
	{
		Git git = initialSetup();
		
		// create new branch and checkout
		git.checkout().setCreateBranch(true).setName("my-branch").call();
		
		// do some changes and commit
		editFile(new File(existingDir, "test1.txt"));
		editFile(new File(existingDir, "test2.txt"));
		
		git.add().addFilepattern(".").call();
		git.commit().setMessage("some changes on branch").call();
		
		// commit some more changes
		editFile(new File(existingDir, "test1.txt"));
		editFile(new File(existingDir, "test2.txt"));
		
		git.add().addFilepattern(".").call();
		git.commit().setMessage("some more changes on branch").call();
		
		// back to master
		git.checkout().setName("master").call();
		
		//cherrypick only the first commit from branch
		Iterable<RevCommit> logs = git.log().add(git.getRepository().resolve("my-branch")).call();
		List<RevCommit> logEntries = StreamSupport.stream(logs.spliterator(), false)
			.collect(Collectors.toList());
		
		assertEquals("some changes on branch", logEntries.get(1).getShortMessage());
		CherryPickResult result = git.cherryPick().include(logEntries.get(1)).call();
		
		assertEquals(CherryPickStatus.OK, result.getStatus());
		assertEquals(1, result.getCherryPickedRefs().size());
	}

	private Git initExisting() throws Exception {

		FileUtils.copyDirectory(new File("src/test/resources"), existingDir);
		return Git.init().setDirectory(existingDir).call();
	}
	
	private Git initialSetup() throws Exception {

		// create a new git repository and copy some files
		Git git = initExisting();

		// add the existing files to git
		git.add().addFilepattern(".").call();
		git.commit().setMessage("initial commit").call();
		
		return git;
	}
	
	private void editFile(final File file) throws IOException {
		
		FileUtils.writeStringToFile(file, UUID.randomUUID().toString() + "\n", Charset.forName("utf-8"), true);
	}
	
	private class LogEntry {
		
		String id;
		Date commitTime;
		String shortMessage;
		String fullMessage;
		String committer;
		
		public LogEntry(RevCommit revCommit) {
			
			this.id = revCommit.getId().getName();
			this.commitTime = new Date(revCommit.getCommitTime() * 1000L);
			this.shortMessage = revCommit.getShortMessage();
			this.fullMessage = revCommit.getFullMessage();
			this.committer = revCommit.getCommitterIdent().getName();
		}

		@Override
		public String toString() {
			return "LogEntry [id=" + id + ", commitTime=" + commitTime + ", shortMessage=" + shortMessage
					+ ", fullMessage=" + fullMessage + ", committer=" + committer + "]";
		}
		
	}
}
