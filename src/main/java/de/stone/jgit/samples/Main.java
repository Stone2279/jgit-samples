package de.stone.jgit.samples;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;

public class Main {

	public static void main(String[] args) throws Exception {

		// create a new git repository. Missing directories are created automatically.
		// Call the init command on a already initialized git repository does nothing and just returns
		// instance of the already existing repository
		File repositoryDir = new File("D:/eigene dateien/Spielwiese/git/jgit");
		Git git = Git.init().setDirectory(repositoryDir).call();
		
		Ref headRef = git.getRepository().findRef(Constants.HEAD);
	}

}
