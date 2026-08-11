# Tyche Prior Integration for Beauti

This document describes the Beauti Prior tab integration for Tyche's prior distributions, including InputEditor classes and PriorProvider for intuitive GUI access.

## Overview

The Tyche Prior integration provides GUI editors for prior distributions in BEAST2 analyses through Beauti. Key components:

1. **InputEditor Classes** - GUI representations for prior parameter editing
2. **PriorProvider Interface** - Integration with Beauti's "Add Prior" button
3. **Service Registration** - Discovery via version.xml

## Implementation

### InputEditor Classes

#### ElementwisePriorInputEditor
- **Class**: `tyche.app.beauti.ElementwisePriorInputEditor`
- **Handles**: `ElementwisePrior`
- **Purpose**: Allows users to configure per-element distributions for multi-dimensional parameters
- **Location**: `src/tyche/app/beauti/ElementwisePriorInputEditor.java`

#### RootTypePriorInputEditor
- **Class**: `tyche.app.beauti.RootTypePriorInputEditor`
- **Handles**: `RootTypePrior`
- **Purpose**: Allows users to configure root type probability distributions
- **Location**: `src/tyche/app/beauti/RootTypePriorInputEditor.java`

### PriorProvider Implementation

#### RootTypePriorProvider
- **Class**: `tyche.app.beauti.RootTypePriorProvider`
- **Interface**: `beastfx.app.beauti.PriorProvider`
- **Function**: Enables "Root Type Prior" option in Add Prior menu
- **Features**:
  - Returns "Root Type Prior" in dropdown
  - Creates RootTypePrior instance with default parameters
  - Integrates RootType and typeProbabilities automatically
- **Location**: `src/tyche/app/beauti/RootTypePriorProvider.java`

### Service Registration

In `version.xml`:
```xml
<service type="beastfx.app.inputeditor.InputEditor">
    <provider classname="tyche.app.beauti.ElementwisePriorInputEditor"/>
    <provider classname="tyche.app.beauti.RootTypePriorInputEditor"/>
</service>

<service type="beastfx.app.beauti.PriorProvider">
    <provider classname="tyche.app.beauti.RootTypePriorProvider"/>
</service>
```

## Building

### Prerequisites
- Java 17+ (Zulu 17 or compatible)
- BEAST 2.7.6+
- Ant build system

### Build Commands
```bash
export JAVA_HOME=/path/to/java17
ant build_jar_all_Tyche_NoJUnitTest
```

## Beauti Validation Steps

### 1. Launch Beauti
```bash
cd /path/to/beast2
java -cp launcher.jar beast.app.beautiApp
```

### 2. Create or Open an XML File
- File → New (or Open existing XML)
- Create basic analysis with alignment and tree

### 3. Verify Prior Tab Access
- Click on "Priors" tab
- Observe "Prior" section with current priors listed

### 4. Access Add Prior Menu
- Click "+ Add Prior" button (green plus icon)
- Window should appear showing available prior options

### 5. Verify "Root Type Prior" Appears
- Look for "Root Type Prior" in the dropdown menu or list
- **Expected behavior**: "Root Type Prior" should be visible as an option
- **If not visible**: Check that version.xml includes PriorProvider registration

### 6. Add Root Type Prior
- Select "Root Type Prior" from the menu
- **Expected behavior**: 
  - New prior instance created with ID "rootTypePrior"
  - RootType component automatically configured
  - typeProbabilities parameter set to "0.5 0.5" (default)

### 7. Edit Prior Parameters
- Click on the newly added prior row
- **Expected behavior**: RootTypePriorInputEditor should render
- Verify the following fields appear:
  - "arg" input (reference to RootType)
  - "typeProbabilities" parameter field (RealParameter)
  - Parameter values are editable

### 8. Test ElementwisePrior Editor
- Add a parameter-based prior that supports ElementwisePrior
- When selected, ElementwisePriorInputEditor should render
- Verify parameter values can be edited

### 9. Export XML
- File → Export XML
- Open exported file and verify:
  - RootTypePrior element present
  - typeProbabilities values saved correctly
  - Prior correctly connected to state and likelihood

## Testing

### Unit Tests

Test file: `src/test/tyche/app/beauti/TychePriorInputEditorTest.java`

Run tests:
```bash
export JAVA_HOME=/path/to/java17
ant build_jar_all_Tyche
```

### Test Coverage
- InputEditor instantiation for both editors
- Correct type() method returns
- PriorProvider instantiation and description
- PriorProvider.canProvidePrior() returns true

### Manual Test Cases

| Test Case | Steps | Expected Result |
|-----------|-------|-----------------|
| Prior Tab Access | Launch Beauti, click Priors tab | Priors tab loads without errors |
| Add Prior Button | Click "+ Add Prior" | Menu appears with prior options |
| Root Type Prior Visibility | Check dropdown for "Root Type Prior" | "Root Type Prior" is listed |
| Prior Creation | Select and add "Root Type Prior" | New prior instance created |
| Parameter Editing | Edit prior parameters in GUI | Parameters update in XML |
| XML Export | File → Export XML | Exported XML contains priors with correct values |

## File Structure

```
src/tyche/app/beauti/
├── ElementwisePriorInputEditor.java       (41 lines)
├── RootTypePriorInputEditor.java          (41 lines)
└── RootTypePriorProvider.java             (72 lines)

src/test/tyche/app/beauti/
└── TychePriorInputEditorTest.java         (80 lines, 9 test methods)

version.xml                                (updated with service entries)
```

**Total changes**: 4 files, ~234 insertions

## Known Limitations & Future Work

1. **Basic PriorProvider**
   - RootTypePriorProvider creates instances with default parameters
   - Future: Add interactive dialogs for parameter configuration

2. **Test Coverage**
   - Unit tests verify instantiation and basic properties
   - Integration tests with Beauti not yet implemented

3. **UI Customization**
   - Current implementation uses default BEASTObjectInputEditor UI
   - Future: Custom UI layouts for better parameter organization

## Troubleshooting

### "Root Type Prior" Not Appearing in Add Prior Menu

**Possible Causes**:
1. version.xml not correctly updated
2. PriorProvider not compiled or in classpath
3. Beauti not restarted after build

**Solution**:
1. Verify version.xml has PriorProvider service entry
2. Rebuild with `ant build_jar_all_Tyche`
3. Restart Beauti

### Prior Parameters Not Editable

**Possible Cause**: RootTypePriorInputEditor not registered

**Solution**:
1. Check version.xml includes InputEditor service
2. Verify RootTypePriorInputEditor compiled successfully
3. Restart Beauti

## References

- [BeastFX PriorProvider Documentation](https://beast.community/)
- [BEAST 2 Prior Documentation](https://beast.community/priors)
- [Tyche GitHub Repository](https://github.com/hoehnlab/tyche)
- Fielding et al. (2025) - TyCHE publication

## Contact

For issues with prior integration in Beauti:
- GitHub: https://github.com/hoehnlab/tyche
- BEAST Discourse: https://discourse.beast.community/
