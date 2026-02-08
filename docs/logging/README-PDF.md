# Download End-to-End Logging Doc as PDF

The document *End-to-End Logging System Explained* is available in two forms:

- **Markdown:** `END_TO_END_LOGGING_SYSTEM_EXPLAINED.md`
- **Print-friendly HTML:** `END_TO_END_LOGGING_SYSTEM_EXPLAINED.html` (same content, ready for PDF)

## How to get a PDF

### Option 1: From the HTML file

1. Open **`END_TO_END_LOGGING_SYSTEM_EXPLAINED.html`** in a browser (double-click or drag into Chrome/Firefox/Edge).
2. Press **Ctrl+P** (Windows/Linux) or **Cmd+P** (Mac).
3. Choose **Save as PDF** or **Print to PDF** as the destination.
4. Click Save. You now have a downloadable PDF with the full document.

The HTML is self-contained and tuned for printing (margins, page breaks). The instruction box at the top is hidden when printing.

### Option 2: Regenerate the HTML

If you edit the Markdown and want to refresh the HTML:

```bash
cd docs/logging
python3 md_to_print_html.py
```

Then open the generated `END_TO_END_LOGGING_SYSTEM_EXPLAINED.html` and use Option 1 to save as PDF.

### Option 3: Using Pandoc (if installed)

If you have [Pandoc](https://pandoc.org/) installed:

```bash
cd docs/logging
pandoc END_TO_END_LOGGING_SYSTEM_EXPLAINED.md -o END_TO_END_LOGGING_SYSTEM_EXPLAINED.pdf
```

This produces a PDF directly from the Markdown.
