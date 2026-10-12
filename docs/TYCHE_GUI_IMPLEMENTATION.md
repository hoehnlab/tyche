# Tyche GUI Implementation for Beauti

This document describes the Beauti GUI implementation for the Tyche plugin, including InputEditor classes, template fragments, and how to build, run, and test the components.

## Overview

The Tyche Beauti GUI provides an intuitive interface for specifying Tyche model parameters in BEAST2 analyses. It includes:

1. **InputEditor Classes** - Specialized GUI editors for Tyche parameter types
2. **Template Fragments** - XML templates defining Tyche model components for Beauti
3. **Service Registration** - Service entries in `version.xml` for plugin discovery

## Architecture

### InputEditor Classes

InputEditors extend `beastfx.app.inputeditor.BEASTObjectInputEditor` to provide custom GUI representations for Tyche classes. Key implementations:

- **TycheClockModelInputEditor** - Handles `AbstractTycheTypeLinkedClockModel` and subclasses
  - Supports: TycheInstantSwitchClockModel, TycheExpectedOccupancyClockModel, TycheMixedSwitchClockModel
  - Manages parameter editing for type-linked mutation rates and node type assignments

- **AncestralTypeLikelihoodInputEditor** - Handles `AncestralTypeLikelihood`
  - Provides UI for ancestral type reconstruction parameters

- **TycheSubstitutionModelInputEditor** - Handles `TycheSVSGeneralSubstitutionModel`
  - Manages substitution model configuration

### Template Fragments

The `TyCHE-beauti-template.xml` file defines Beauti template subtemplates for:

1. **Clock Model Templates** - Three variants of Tyche clock models:
   - TycheInstantSwitchClock
   - TycheExpectedOccupancyClock
   - TycheMixedSwitchClock

2. **Likelihood Templates** - AncestralTypeLikelihood definition

Each template specifies:
- XML element structure for model instantiation
- Input parameters (RealParameter, IntegerParameter)
- Operators for MCMC sampling
- Connections to state, priors, and logging

### Service Registration

In `version.xml`, services are registered to allow Beauti/BeastFX to discover and instantiate GUI editors:

```xml
<service type="beastfx.app.inputeditor.InputEditor">
    <provider classname="tyche.app.beauti.TycheClockModelInputEditor"/>
    <provider classname="tyche.app.beauti.AncestralTypeLikelihoodInputEditor"/>
    <provider classname="tyche.app.beauti.TycheSubstitutionModelInputEditor"/>
</service>
```

## Building

### Prerequisites

- Java 17+ (Zulu 17 or compatible)
- BEAST 2.7.6+
- BEAST Classic 1.6.2+
- Ant build system

### Build Commands

```bash
# From the repository root
export JAVA_HOME=/path/to/java17
ant build_jar_all_Tyche_NoJUnitTest

# With unit tests
ant build_jar_all_Tyche
```

Build artifacts are generated in `build/dist/`:
- `TyCHE.package.jar` - Compiled classes
- `TyCHE.src.jar` - Source code

### Build Output

A successful build produces:
```
BUILD SUCCESSFUL
Total time: 3 seconds
```

JAR files are ready for packaging as a BEAST2 add-on.

## Running Beauti with Tyche

### Installation

1. Build the Tyche package:
   ```bash
   export JAVA_HOME=/path/to/java17
   ant package
   ```

2. Copy the generated package to your BEAST2 installation:
   ```bash
   cp release/package/TyCHE.v*.zip /path/to/beast2/packages/
   ```

### Launch Beauti

```bash
cd /path/to/beast2
java -cp launcher.jar beast.app.beautiApp
```

### Manual Testing Checklist

1. **Plugin Loading**
   - [ ] Beauti starts without errors
   - [ ] Tyche package appears in package list (if viewing installed packages)
   - [ ] No exceptions in console output

2. **Clock Model Selection**
   - [ ] Open File → Template → select a Tyche template option
   - [ ] Verify clock model dropdown includes Tyche models:
     - [ ] TycheInstantSwitchClock
     - [ ] TycheExpectedOccupancyClock
     - [ ] TycheMixedSwitchClock

3. **InputEditor UI**
   - [ ] Select a Tyche clock model
   - [ ] Verify InputEditor appears for the selected model
   - [ ] Type-linked rates parameter field appears
   - [ ] Node types parameter field appears
   - [ ] Parameter values are editable

4. **XML Generation**
   - [ ] File → Export XML
   - [ ] Verify XML contains:
     - [ ] Tyche clock model class specification
     - [ ] typeLinkedRates parameter definition
     - [ ] nodeTypes parameter definition
     - [ ] Operators for parameter sampling

5. **Template Integration**
   - [ ] Verify template operators appear in operators panel
   - [ ] Priors for Tyche parameters appear in priors panel
   - [ ] Logging entries for Tyche parameters appear

## Testing

### Unit Tests

Run unit tests with:

```bash
export JAVA_HOME=/path/to/java17
ant build_jar_all_Tyche
```

Test classes are located in `src/test/tyche/app/beauti/`:
- `TycheInputEditorTest.java` - Tests for InputEditor instantiation and type handling

### Test Coverage

Current tests verify:
- InputEditor classes instantiate without errors
- InputEditor `type()` method returns correct target classes
- Service registration in version.xml includes all editors

### Running Tests Individually

```bash
export JAVA_HOME=/path/to/java17
cd /path/to/tyche/repo
javac -cp build/dist/TyCHE.package.jar src/test/tyche/app/beauti/TycheInputEditorTest.java
java -cp build:build/dist/TyCHE.package.jar org.junit.runner.JUnitCore test.tyche.app.beauti.TycheInputEditorTest
```

## File Structure

```
src/tyche/app/beauti/
├── TycheClockModelInputEditor.java
├── AncestralTypeLikelihoodInputEditor.java
└── TycheSubstitutionModelInputEditor.java

fxtemplates/
└── TyCHE-beauti-template.xml

src/test/tyche/app/beauti/
└── TycheInputEditorTest.java

version.xml (updated with InputEditor service entries)
```

## Known Limitations & Future Work

1. **Basic Implementation**
   - Current InputEditors extend `BEASTObjectInputEditor` with default behavior
   - Custom UI layouts (per-category fields, trait selectors) implemented in other editors can be added

2. **Advanced Features Not Yet Implemented**
   - Custom UI for type selection and per-type parameter configuration
   - Visual tree representation with type annotations
   - Parameter validation UI

3. **Testing**
   - Integration tests with Beauti (requires JavaFX environment)
   - End-to-end XML generation tests

## References

- [BEAST 2 Documentation](https://beast.community/2.7/)
- [BeastFX InputEditor Architecture](https://beast.community/)
- [Tyche GitHub Repository](https://github.com/hoehnlab/tyche)
- Fielding et al. (2025) - TyCHE enables time-resolved lineage tracing

## Development Notes

### Modifying InputEditors

To extend InputEditor functionality:

1. Subclass `BEASTObjectInputEditor`
2. Override `type()` to return the target BEAST class
3. Override `init()` to build custom UI components
4. Register in `version.xml` under `beastfx.app.inputeditor.InputEditor` service

### Adding Template Subtemplates

To add new Tyche model templates:

1. Add a new `<subtemplate>` block in `TyCHE-beauti-template.xml`
2. Specify the class, mainid, and hmc (hidden model components) attributes
3. Include XML definition in CDATA section
4. Add `<connect>` elements to wire into MCMC analysis

## Contact

For issues or questions about the Tyche GUI implementation, please refer to:
- Tyche GitHub: https://github.com/hoehnlab/tyche
- BEAST 2 Discussions: https://beast.community/
