package de.sebastianruziczka.compiler.gnucobol

import de.sebastianruziczka.CobolExtension
import de.sebastianruziczka.compiler.api.CompileJob
import de.sebastianruziczka.compiler.api.CompileStandard
import de.sebastianruziczka.compiler.api.ExecutableCompilerBuilder
import de.sebastianruziczka.process.ProcessWrapper
import de.sebastianruziczka.compiler.api.CompilerBuilder

class GnuExecutableCompilerBuilder implements ExecutableCompilerBuilder {

	private CompileStandard standard = CompileStandard.none
	private List<String> includePaths = new ArrayList<String>()
	private List<String> dependencyPaths = new ArrayList<String>()
	private CobolExtension configuration
	private CompilerBuilder compilerBuilder

	public GnuExecutableCompilerBuilder(CobolExtension configuration, CompilerBuilder compilerBuilder) {
		this.configuration = configuration
		this.compilerBuilder = compilerBuilder;
	}

	@Override
	public ExecutableCompilerBuilder setCompileStandard(CompileStandard standard) {
		GnuExecutableCompilerBuilder copy = new GnuExecutableCompilerBuilder(this.configuration, this.compilerBuilder)
		copy.standard = standard
		copy.dependencyPaths = this.dependencyPaths
		copy.includePaths = this.includePaths

		return copy
	}

	@Override
	public ExecutableCompilerBuilder addIncludePath(String path) {
		GnuExecutableCompilerBuilder copy = new GnuExecutableCompilerBuilder(this.configuration, this.compilerBuilder)
		copy.standard = this.standard
		copy.dependencyPaths = this.dependencyPaths
		copy.includePaths = new LinkedList(this.includePaths)
		copy.includePaths.add(path)

		return copy
	}

	@Override
	public ExecutableCompilerBuilder addDependencyPath(String path) {
		GnuExecutableCompilerBuilder copy = new GnuExecutableCompilerBuilder(this.configuration, this.compilerBuilder)
		copy.standard = this.standard
		copy.dependencyPaths = new LinkedList(this.dependencyPaths)
		copy.dependencyPaths.add(path)
		copy.includePaths = this.includePaths
		return copy
	}

	@Override
	public ExecutableCompilerBuilder addDependencyPaths(List<String> list) {
		GnuExecutableCompilerBuilder copy = new GnuExecutableCompilerBuilder(this.configuration, this.compilerBuilder)
		copy.standard = this.standard
		copy.dependencyPaths = new LinkedList(this.dependencyPaths)

		for(String path: list){
			copy.dependencyPaths.add(path)
		}

		copy.includePaths = this.includePaths
		return copy
	}

	@Override
	public CompileJob setTargetAndBuild(String targetPath) {
		return new GnuCompileJob(this.standard, this.includePaths, this.dependencyPaths, targetPath, this.configuration, this.compilerBuilder)
	}
}

class GnuCompileJob implements CompileJob  {

	private CompileStandard standard = CompileStandard.none
	private List<String> dependencyPaths
	private List<String> includePaths
	private List<String> additionalOptions = new ArrayList<>()

	private String target
	private String outputPath = null
	private CompilerBuilder compilerBuilder

	private CobolExtension configuration

	public GnuCompileJob(CompileStandard standard, List<String> includePaths, List<String> dependencyPaths, String target, CobolExtension configuration, CompilerBuilder compilerBuilder){
		this.standard = standard
		this.includePaths = includePaths
		this.target = target
		this.dependencyPaths = dependencyPaths
		this.configuration = configuration
		this.compilerBuilder = compilerBuilder
	}

	@Override
	public CompileJob setExecutableDestinationPath(String outputPath) {
		GnuCompileJob copy = new GnuCompileJob(this.standard, this.includePaths, this.dependencyPaths, this.target, this.configuration, this.compilerBuilder)
		copy.outputPath = outputPath
		copy.additionalOptions = this.additionalOptions
		return copy
	}

	@Override
	public int execute(String processName) {
		def args = [this.compilerBuilder.getBaseCompilerCommand()]

		args << new GnuCobolLoglevelOptionResolver().resolve(this.configuration)

		args << '-x'

		if (this.standard != CompileStandard.none) {
			args << '-std=' + this.standard.toString()
		}

		for (String dependency : this.includePaths) {
			args << '-I'
			args << dependency
		}

		String logPath = this.target + '_COMPILE.LOG'
		if (this.outputPath) {
			args << '-o'
			args << this.outputPath
			logPath = this.outputPath + '_COMPILE.LOG'
		}

		if ( this.additionalOptions.size() > 0) {
			for (String option : this.additionalOptions) {
				args <<  '-' + option
			}
		}

		args << this.target

		if (this.dependencyPaths) {
			args += dependencyPaths
		}

		File folder = new File(this.target).getParentFile()
		ProcessWrapper processWrapper = new ProcessWrapper(args, folder, processName, logPath)

		return processWrapper.exec()
	}



	@Override
	public CompileJob addAdditionalOption(String option) {
		GnuCompileJob copy = new GnuCompileJob(this.standard, this.includePaths, this.dependencyPaths, this.target, this.configuration, this.compilerBuilder)
		copy.outputPath = this.outputPath
		copy.additionalOptions = new LinkedList(this.additionalOptions)
		copy.additionalOptions.add(option)
		return copy
	}

	@Override
	public CompileJob addCodeCoverageOption() {
		return this.addAdditionalOption('ftraceall');
	}
}