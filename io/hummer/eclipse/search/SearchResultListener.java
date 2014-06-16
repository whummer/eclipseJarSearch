package io.hummer.eclipse.search;

import io.hummer.eclipse.search.JarSearcher.FileSearchMatch;

public interface SearchResultListener {

	void onResult(FileSearchMatch match);
	
	boolean stillRunning();
	
	void searchFinished();
}
