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

import org.apache.log4j.Level;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Create a Spoon launcher from a maven pom file
 */
public class MavenLauncher extends Launcher {
	private String m2RepositoryPath;
	private SOURCE_TYPE sourceType;

	/**
	 * The type of source to consider in the model
	 */
	public enum SOURCE_TYPE {
		// only the main code of the application
		APP_SOURCE,
		// only the tests of the application
		TEST_SOURCE,
		// all the sources
		ALL_SOURCE
	}

	public MavenLauncher(String mavenProject, SOURCE_TYPE sourceType) {
		this(mavenProject, Paths.get(System.getProperty("user.home"), ".m2", "repository").toString(), sourceType);
	}

	/**
	 *
	 * @param mavenProject the path to the root of the project
	 * @param m2RepositoryPath the path to the m2repository
	 */
	public MavenLauncher(String mavenProject, String m2RepositoryPath, SOURCE_TYPE sourceType) {
		super();
		this.m2RepositoryPath = m2RepositoryPath;
		this.sourceType = sourceType;

		File mavenProjectFile = new File(mavenProject);
		if (!mavenProjectFile.exists()) {
			throw new SpoonException(mavenProject + " does not exist.");
		}

		InheritanceModel model;
		try {
			model = readPOM(mavenProject, null);
		} catch (Exception e) {
			throw new SpoonException("Unable to read the pom", e);
		}
		if (model == null) {
			throw new SpoonException("Unable to create the model, pom not found?");
		}

		// app source
		if (SOURCE_TYPE.APP_SOURCE == sourceType || SOURCE_TYPE.ALL_SOURCE == sourceType) {
			List<File> sourceDirectories = model.getSourceDirectories();
			for (File sourceDirectory : sourceDirectories) {
				this.addInputResource(sourceDirectory.getAbsolutePath());
			}
		}

		// test source
		if (SOURCE_TYPE.TEST_SOURCE == sourceType || SOURCE_TYPE.ALL_SOURCE == sourceType) {
			List<File> testSourceDirectories = model.getTestDirectories();
			for (File sourceDirectory : testSourceDirectories) {
				this.addInputResource(sourceDirectory.getAbsolutePath());
			}
		}

		// dependencies
		List<File> dependencies = model.getDependencies(false, new HashSet<>(), new HashMap<>());
		String[] classpath = new String[dependencies.size()];
		for (int i = 0; i < dependencies.size(); i++) {
			File file = dependencies.get(i);
			classpath[i] = file.getAbsolutePath();
		}
		this.getModelBuilder().setSourceClasspath(classpath);

		// compliance level
		this.getEnvironment().setComplianceLevel(model.getSourceVersion());
	}

	/**
	 * Extract the information from the pom
	 * @param path the path to the pom
	 * @param parent the parent pom
	 * @return the extracted model
	 * @throws IOException when the file does not exist
	 * @throws XmlPullParserException when the file is corrupted
	 */
	private InheritanceModel readPOM(String path, InheritanceModel parent) throws IOException, XmlPullParserException {
		if (!path.endsWith(".xml") && !path.endsWith(".pom")) {
			path = Paths.get(path, "pom.xml").toString();
		}
		File pomFile = new File(path);
		if (!pomFile.exists()) {
			return null;
		}
		MavenXpp3Reader pomReader = new MavenXpp3Reader();
		try (FileReader reader = new FileReader(pomFile)) {
			Model model = pomReader.read(reader);
			InheritanceModel inheritanceModel = new InheritanceModel(model, parent, pomFile.getParentFile());
			for (String module : model.getModules()) {
				if (path.contains(m2RepositoryPath)) {
					InheritanceModel modulePom = readPOM(path.replaceAll(model.getArtifactId(), module), inheritanceModel);
					if (modulePom != null) {
						inheritanceModel.addModule(modulePom);
					}
				} else {
					inheritanceModel.addModule(readPOM(Paths.get(pomFile.getParent(), module).toString(), inheritanceModel));
				}
			}
			return inheritanceModel;
		}
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
			// if possible, build the parent model from the relative path
			if (parent == null && model.getParent() != null) {
				try {
					File parentPath = new File(directory, model.getParent().getRelativePath());
					this.parent = readPOM(parentPath.getPath(), null);
				} catch (Exception e) {
					LOGGER.debug("Parent model cannot be resolved: " + e.getMessage());
				}
			}
		}

		public void addModule(InheritanceModel module) {
			modules.add(module);
		}

		public Model getModel() {
			return model;
		}

		/**
		 * Get the parent model
		 * @return the parent model
		 */
		public InheritanceModel getParent() {
			return parent;
		}

		/**
		 * Get the list of source directories of the project
		 * @return the list of source directories
		 */
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
			File generatedSource = Paths.get(directory.getAbsolutePath(), "target", "generated-sources").toFile();
			if (generatedSource.exists()) {
				output.add(generatedSource);
			}
			for (InheritanceModel module : modules) {
				output.addAll(module.getSourceDirectories());
			}
			return output;
		}

		/**
		 * Get the list of test directories of the project
		 * @return the list of test directories
		 */
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
			File generatedSource = Paths.get(directory.getAbsolutePath(), "target", "generated-test-sources").toFile();
			if (generatedSource.exists()) {
				output.add(generatedSource);
			}
			for (InheritanceModel module : modules) {
				output.addAll(module.getTestDirectories());
			}
			return output;
		}

		/**
		 * Extract the variable from a string
		 */
		private String extractVariable(String value) {
			if (value != null && value.startsWith("$")) {
				value = getProperty(value.substring(2, value.length() - 1));
			}
			return value;
		}

		private Set<File> getDependencies(String groupId, String artifactId, String version, String scope, boolean isOptional, boolean isLib, Set<String> hierarchy, Map<String, String> dependencyManagements) {
			Set<File> output = new HashSet<>();
			if (version == null) {
				if (dependencyManagements.containsKey(groupId + ":" + artifactId)) {
					version = dependencyManagements.get(groupId + ":" + artifactId);
				} else {
					return output;
				}
			}
			groupId = groupId.replace(".", "/");
			// TODO: Handle range version
			version = extractVariable(version);
			if (version == null) {
				LOGGER.warn("A dependency version cannot be resolved: " + groupId + ":" + artifactId + ":" + version);
				return output;
			}
			if (version.startsWith("[")) {
				version = version.substring(1, version.indexOf(','));
			}
			// pass only the optional dependency if it's in a library dependency
			if (isLib && isOptional) {
				return output;
			}

			// ignore test dependencies for app source code
			if ("test".equals(scope) && SOURCE_TYPE.APP_SOURCE == sourceType) {
				return output;
			}
			// ignore not transitive dependencies
			if (isLib && ("test".equals(scope) || "provided".equals(scope))) {
				LOGGER.log(Level.WARN, "Dependency ignored (scope: provided or test): " + groupId + ":" + artifactId + ":" + version);
				return output;
			}
			String fileName = artifactId + "-" + version;
			Path depPath = Paths.get(m2RepositoryPath, groupId, artifactId, version);
			File depFile = depPath.toFile();
			if (depFile.exists()) {
				File jarFile = Paths.get(depPath.toString(), fileName + ".jar").toFile();
				if (jarFile.exists()) {
					output.add(jarFile);

					String depKey = groupId + ":" + artifactId;
					if (!dependencyManagements.containsKey(depKey)) {
						dependencyManagements.put(depKey, version);
					}
				} else {
					// if the a dependency is not found, uses the no classpath mode
					getEnvironment().setNoClasspath(true);
				}

				try {
					InheritanceModel dependencyModel = readPOM(Paths.get(depPath.toString(), fileName + ".pom").toString(), null);
					output.addAll(dependencyModel.getDependencies(true, hierarchy, dependencyManagements));
				} catch (Exception ignore) {
					// ignore the dependencies of the dependency
				}
			} else {
				// if the a dependency is not found, uses the no classpath mode
				getEnvironment().setNoClasspath(true);
			}
			return output;
		}
		/**
		 * Get the list of dependencies available in the local maven repository
		 *
		 * @param isLib: If false take dependency of the main project; if true, take dependencies of a library of the project
		 * @return the list of  dependencies
		 */
		public List<File> getDependencies(boolean isLib, Set<String> hierarchy, Map<String, String> dependencyManagements) {
			Set<File> output = new HashSet<>();

			String modelKey = model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion();
			if (hierarchy.contains(modelKey)) {
				return new ArrayList<>(output);
			}
			hierarchy.add(modelKey);

			DependencyManagement dependencyManagement = model.getDependencyManagement();
			if (dependencyManagement != null) {
				List<Dependency> dependencies = dependencyManagement.getDependencies();
				for (Dependency dependency : dependencies) {
					if ("import".equals(dependency.getScope())) {
						getDependencies(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), "parent", false, isLib, hierarchy, dependencyManagements);
					} else {
						String depKey = dependency.getGroupId() + ":" + dependency.getArtifactId();
						if (!dependencyManagements.containsKey(depKey)) {
							dependencyManagements.put(depKey, extractVariable(dependency.getVersion()));
						}
					}
				}
			}

			// add the parent has a dependency
			Parent parent = model.getParent();
			if (parent != null) {
				output.addAll(getDependencies(parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), "parent", false, isLib, hierarchy, dependencyManagements));
			}

			List<Dependency> dependencies = model.getDependencies();
			for (Dependency dependency : dependencies) {
				output.addAll(getDependencies(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getScope(), dependency.isOptional(), isLib, hierarchy, dependencyManagements));
			}

			for (InheritanceModel module : modules) {
				output.addAll(module.getDependencies(isLib, hierarchy, dependencyManagements));
			}
			return new ArrayList<>(output);
		}

		/**
		 * Get the value of a property
		 * @param key the key of the property
		 * @return the property value if key exists or null
		 */
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

		/**
		 * Get the source version of the project
		 * @return the source version of the project
		 */
		public int getSourceVersion() {
			if (model.getBuild() != null) {
				for (Plugin plugin : model.getBuild().getPlugins()) {
					if (!"maven-compiler-plugin".equals(plugin.getArtifactId())) {
						continue;
					}
					Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
					Xpp3Dom source = configuration.getChild("source");
					if (source != null) {
						return Integer.parseInt(extractVariable(source.getValue()).substring(2));
					}
					break;
				}
			}
			String javaVersion = getProperty("java.version");
			if (javaVersion != null) {
				return Integer.parseInt(extractVariable(javaVersion).substring(2));
			}
			javaVersion = getProperty("java.src.version");
			if (javaVersion != null) {
				return Integer.parseInt(extractVariable(javaVersion).substring(2));
			}
			javaVersion = getProperty("maven.compiler.source");
			if (javaVersion != null) {
				return Integer.parseInt(extractVariable(javaVersion).substring(2));
			}
			javaVersion = getProperty("maven.compile.source");
			if (javaVersion != null) {
				return Integer.parseInt(extractVariable(javaVersion).substring(2));
			}
			// return the current compliance level of spoon
			return getEnvironment().getComplianceLevel();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion());
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
