package net.hummer.eclipse.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class JarSearcher {

	private static final String JDT_NATURE = "org.eclipse.jdt.core.javanature";

	IWorkspace workspace;

	public JarSearcher(IWorkspace workspace) {
		this.workspace = workspace;
	}

	public Map<IJavaProject,Set<File>> getClasspathJarFiles() throws Exception {
		return getClasspathJarFiles(false);
	}
	public Map<IJavaProject,Set<File>> getClasspathJarFiles(boolean includeJRE) throws Exception {
		Map<IJavaProject,Set<File>> result = new HashMap<IJavaProject, Set<File>>();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();
		for (IProject project : projects) {
			// Only work on open projects with the Java nature
			if (project.isOpen() && project.isNatureEnabled(JDT_NATURE)) {
				IJavaProject javaProject = JavaCore.create(project);
				Set<File> map = new HashSet<File>();
				result.put(javaProject, map);
				IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
				for (IClasspathEntry entry : entries) {
					//System.out.println(entry + " - " + entry.getEntryKind());
					if(entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
						if(entry.getPath() != null) {
							IPath p = entry.getPath();
							if(includeJRE || 
									!(p.toString().contains("jre") && 
											p.toString().contains("lib"))) {
								File fileToAdd = null;
								if(p.isAbsolute()) {
									IFile f = root.getFile(p);
									if(f != null && f.getLocation() != null) {
										fileToAdd = getPath(f.getLocation());
									} else {
										fileToAdd = getPath(p);
									}
								} else {
									fileToAdd = getPath(root.getFile(p).getLocation());
								}
								if(fileToAdd != null) {
									System.out.println(fileToAdd);
									map.add(fileToAdd);
								}
							}
						}
					}
//					if(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
//						System.out.println(entry.getReferencingEntry());
//						System.out.println(Arrays.asList(entry.getExtraAttributes()));
//						System.out.println(entry.getClass());
//						org.eclipse.jdt.internal.core.ClasspathEntry e = (org.eclipse.jdt.internal.core.ClasspathEntry)entry;
//					}
				}
			}
		}
		return result;
	}

	public List<File> getJarFileContaining(String toSearch) throws Exception {
		List<File> result = new LinkedList<File>();
		for(Entry<IJavaProject,Set<File>> e : getClasspathJarFiles().entrySet()) {
			for(File f : e.getValue()) {
				if(jarFileContains(f, toSearch)) {
					result.add(f);
				}
			}
		}
		return result;
	}

	private boolean jarFileContains(File jarFile, String toSearch) throws Exception {
		return !getJarEntriesContaining(jarFile, toSearch).isEmpty();
	}

	public static class FileSearchMatch {
		public int start;
		public int end;
		public JarFileEntry file;
		public String match;
		public String getMatchWithSurroundingContent(int surroundingLength) {
			String content = file.contentString;
			int s = start - Math.min(surroundingLength, start);
			int e = end + Math.min(surroundingLength, content.length() - end);
			return content.substring(s, e);
		}
	}

	public static class JarFileEntry {
		public File jarFile;
		public String name;
		public byte[] content;
		public String contentString;
		public final List<FileSearchMatch> matches = new LinkedList<>();
		public final Set<String> projectNames = new HashSet<String>();
		public String dumpMatches(int surroundingLength) {
			String r = "";
			for(int i = 0; i < matches.size(); i ++) {
				String m = matches.get(i).getMatchWithSurroundingContent(surroundingLength);
				r += m;
				System.out.println(m);
				if(i < matches.size() - 1) {
					 r += " ... \n";
				}
			}
			return r.replace("\n", " ");
		}
	}

	public List<JarFileEntry> getJarEntriesContaining(String toSearch) throws Exception {
		return getJarEntriesContaining(toSearch, false);
	}

	public List<JarFileEntry> getJarEntriesContaining(String toSearch, boolean includeJRE) throws Exception {
		List<JarFileEntry> result = new LinkedList<JarFileEntry>();
		Map<File,List<JarFileEntry>> results = new HashMap<File, List<JarFileEntry>>();
		for(Entry<IJavaProject,Set<File>> e : getClasspathJarFiles(includeJRE).entrySet()) {
			for(File jarFile : e.getValue()) {
				if(!results.containsKey(jarFile)) {
					List<JarFileEntry> entries = getJarEntriesContaining(jarFile, toSearch);
					result.addAll(entries);
					results.put(jarFile, entries);
				}
				for(JarFileEntry entry : results.get(jarFile)) {
					entry.projectNames.add(e.getKey().getProject().getName());
				}
			}
		}
		return result;
	}

	public List<JarFileEntry> getJarEntriesContaining(File jarFile, String toSearch) throws Exception {
		List<JarFileEntry> result = new LinkedList<JarFileEntry>();
		JarFile file = new JarFile(jarFile);
		for(JarEntry e : Collections.list(file.entries())) {
			InputStream is = file.getInputStream(e);
			String content = read(is);
			if(content.contains(toSearch) || content.matches(toSearch)) {
				JarFileEntry entry = new JarFileEntry();
				entry.name = e.getName();
				entry.jarFile = jarFile;
				entry.content = content.getBytes();
				entry.contentString = content.replaceAll("\\p{C}", "?");
				result.add(entry);
				Pattern p = Pattern.compile(toSearch);
				Matcher m = p.matcher(content);
				while(m.find()) {
					FileSearchMatch match = new FileSearchMatch();
					match.start = m.start();
					match.end = m.end();
					match.match = m.group();
					match.file = entry;
					entry.matches.add(match);
				}
			}
		}
		return result;
	}

	private File getPath(IPath path) {
		return path.toFile();
	}

	private String read(InputStream is) throws IOException {
		StringBuilder b = new StringBuilder();
		String line = null;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		while((line = r.readLine()) != null) {
			b.append(line);
		}
		return b.toString();
	}
}
