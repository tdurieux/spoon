/**
 * Copyright (C) 2006-2017 INRIA and contributors
 * Spoon - http://spoon.gforge.inria.fr/
 *
 * This software is governed by the CeCILL-C License under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at http://www.cecill.info.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the CeCILL-C License for more details.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package spoon;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Create a Spoon launcher from a maven pom file
 */
public class MavenLauncher extends Launcher {
	private String m2RepositoryPath;

	public MavenLauncher(String pomPatch, boolean includeTest) {
		this(pomPatch, Paths.get(System.getProperty("user.home"), ".m2", "repository").toString(), includeTest);
	}

	/**
	 *
	 * @param projectRoot the path to the root of the project (the folder that contains the pom)
	 * @param m2RepositoryPath the path to the m2repository
	 */
	public MavenLauncher(String projectRoot, String m2RepositoryPath, boolean includeTest) {
		this.m2RepositoryPath = m2RepositoryPath;

		if (!new File(projectRoot).isDirectory()) {
			throw new SpoonException(projectRoot + " has to be a folder");
		}

		InheritanceModel model;
		try {
			model = readPOM(projectRoot, null);
		} catch (Exception e) {
			throw new SpoonException("Unable to read the pom", e);
		}
		if (model == null) {
			throw new SpoonException(" Unable to create the model, pom not found?");
		}

		// source
		List<File> sourceDirectories = model.getSourceDirectories();
		for (File sourceDirectory : sourceDirectories) {
			this.addInputResource(sourceDirectory.getAbsolutePath());
		}

		// test
		if (includeTest) {
			List<File> testSourceDirectories = model.getTestDirectories();
			for (File sourceDirectory : testSourceDirectories) {
				this.addInputResource(sourceDirectory.getAbsolutePath());
			}
		}

		// dependencies
		List<File> dependencies = model.getDependencies();
		String[] classpath = new String[dependencies.size()];
		for (int i = 0; i < dependencies.size(); i++) {
			File file = dependencies.get(i);
			classpath[i] = file.getAbsolutePath();
		}
		this.getModelBuilder().setSourceClasspath(classpath);
	}


	private InheritanceModel readPOM(String path, InheritanceModel parent) throws IOException, XmlPullParserException {
		File parentPOM = Paths.get(path, "pom.xml").toFile();
		if (!parentPOM.exists()) {
			return null;
		}
		MavenXpp3Reader pomReader = new MavenXpp3Reader();
		Model model = pomReader.read(new FileReader(parentPOM));
		InheritanceModel inheritanceModel = new InheritanceModel(model, parent, new File(path));
		for (String module : model.getModules()) {
			inheritanceModel.addModule(readPOM(Paths.get(path, module).toString(), inheritanceModel));
		}
		return inheritanceModel;
	}

	class InheritanceModel {
		private List<InheritanceModel> modules = new ArrayList<>();
		private Model model;
		private InheritanceModel parent;
		private File directory;

		InheritanceModel(Model model, InheritanceModel parent, File directory) {
			this.model = model;
			this.parent = parent;
			this.directory = directory;
		}

		public void addModule(InheritanceModel module) {
			modules.add(module);
		}

		public Model getModel() {
			return model;
		}

		public InheritanceModel getParent() {
			return parent;
		}

		public List<File> getSourceDirectories() {
			List<File> output = new ArrayList<>();
			String sourcePath = null;

			Build build = model.getBuild();
			if (build != null) {
				sourcePath = build.getSourceDirectory();
			}
			if (sourcePath == null) {
				sourcePath = Paths.get(directory.getAbsolutePath(), "src", "main", "java").toString();
			}
			File source = new File(sourcePath);
			if (source.exists()) {
				output.add(source);
			}
			for (InheritanceModel module : modules) {
				output.addAll(module.getSourceDirectories());
			}
			return output;
		}

		public List<File> getTestDirectories() {
			List<File> output = new ArrayList<>();
			String sourcePath = null;

			Build build = model.getBuild();
			if (build != null) {
				sourcePath = build.getTestSourceDirectory();
			}
			if (sourcePath == null) {
				sourcePath = Paths.get(directory.getAbsolutePath(), "src", "test", "java").toString();
			}
			File source = new File(sourcePath);
			if (source.exists()) {
				output.add(source);
			}
			for (InheritanceModel module : modules) {
				output.addAll(module.getTestDirectories());
			}
			return output;
		}

		public List<File> getDependencies() {
			Set<File> output = new HashSet<>();

			List<Dependency> dependencies = model.getDependencies();
			for (Dependency dependency : dependencies) {
				String groupId = dependency.getGroupId().replace(".", "/");
				String version = dependency.getVersion();
				if (version.startsWith("$")) {
					version = getProperty(version.substring(2, version.length() - 1));
				}
				String fileName = dependency.getArtifactId() + "-" + version + ".jar";
				Path depPath = Paths.get(m2RepositoryPath, groupId, dependency.getArtifactId(), version, fileName);
				File jar = depPath.toFile();
				if (jar.exists()) {
					output.add(jar);
				}
			}

			for (InheritanceModel module : modules) {
				output.addAll(module.getDependencies());
			}
			return new ArrayList<>(output);
		}

		private String getProperty(String key) {
			if ("project.version".equals(key)) {
				if (model.getVersion() != null) {
					return model.getVersion();
				}
			}
			String value = model.getProperties().getProperty(key);
			if (value == null) {
				if (parent == null) {
					return null;
				}
				return parent.getProperty(key);
			}
			return value;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(model.getName());
			if (modules.isEmpty()) {
				return sb.toString();
			}
			sb.append(" {\n");
			for (int i = 0; i < modules.size(); i++) {
				InheritanceModel inheritanceModel =  modules.get(i);
				String child = inheritanceModel.toString();
				for (String s : child.split("\n")) {
					sb.append("\t");
					sb.append(s);
					sb.append("\n");
				}
			}
			sb.append("}");
			return sb.toString();
		}
	}
}