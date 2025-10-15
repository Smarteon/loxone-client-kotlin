# Loxone Communication Documentation

This directory contains the Loxone Miniserver documentation converted from the official PDFs to Markdown format.

## Files

- **CommunicatingWithMiniserver.md** - The main communication protocol documentation
- **StructureFile.md** - Structure file documentation
- **UserManagement.md** - User management documentation
- **OperatingModeSchedule.md** - Operating mode schedule documentation
- **PortsDomains.md** - Ports and domains documentation
- **APICommands.md** - API Commands (Konektor) documentation

## Sources

The documentation is converted from the official Loxone PDFs available at:
- https://www.loxone.com/wp-content/uploads/datasheets/CommunicatingWithMiniserver.pdf
- https://www.loxone.com/wp-content/uploads/datasheets/StructureFile.pdf
- https://www.loxone.com/wp-content/uploads/datasheets/UserManagement.pdf
- https://www.loxone.com/wp-content/uploads/datasheets/OperatingModeSchedule.pdf
- https://www.loxone.com/wp-content/uploads/datasheets/Loxone_PortsDomains.pdf
- https://www.loxone.com/wp-content/uploads/datasheets/API-Commands.pdf

## Regenerating the Documentation

To regenerate the markdown documentation from the latest PDF:

1. Go to the [Actions tab](../../actions/workflows/convert-loxone-docs.yml)
2. Click "Run workflow"
3. Wait for the workflow to complete and create a pull request
4. Review and merge the pull request

## Conversion Tool

The conversion is performed using [pymupdf4llm](https://github.com/pymupdf/PyMuPDF-Utilities/tree/main/pymupdf4llm), which provides high-quality PDF to Markdown conversion with good handling of:
- Text extraction
- Tables
- Images
- Formatting preservation

## Why Markdown?

Converting the PDF to Markdown provides several benefits:
- **Version Control**: Track changes to the documentation over time
- **Searchable**: Easy to search through the documentation
- **Accessible**: Can be viewed directly in GitHub
- **Integration**: Can be integrated into documentation sites
- **Editable**: Can be enhanced with additional notes or examples
