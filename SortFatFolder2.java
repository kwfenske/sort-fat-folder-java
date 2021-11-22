/*
  Sort FAT Folder #2 - Sort FAT16/FAT32 Folder in Alphabetical Order
  Written by: Keith Fenske, http://kwfenske.github.io/
  Tuesday, 12 September 2017
  Java class name: SortFatFolder2
  Copyright (c) 2017 by Keith Fenske.  Apache License or GNU GPL.

  This is a Java 1.4 application to sort the directory entries for a FAT16 or
  FAT32 file folder.  Unlike newer file systems (NTFS), the FAT file system
  does not keep directories in alphabetical order.  This is hidden from the
  user by most graphical interfaces, but becomes important on some types of
  distribution media, e.g., USB thumb drives.

  A new folder is created, in the same parent folder as a user's given folder,
  and entries (files and subfolders) are moved to the new folder, one at a
  time, in the desired order.  The old folder, which will now be empty, is
  deleted.  The new folder is renamed the same as the old folder, and will
  likely be out of order in the parent folder.  (There are limits on how much
  cleaning can be done, even manually.)

  DO NOT USE THIS PROGRAM ON SYSTEM FOLDERS.  The only safe folders are those
  you create with your own files and no hidden or system files.  The program
  has no purpose on any file system that maintains an internal sorted order.

  You can't sort root directories with this program, as there is no parent
  folder.  No application can have any of the files, folders, or subfolders
  open (locked).  Windows Vista/7 or later may complain if you move, rename, or
  delete a shared folder.  Some anti-virus products interfere with rapid file
  activity.  After sorting, folders and subfolders lose special attributes
  (hidden, read-only, system) and may lose metadata such as sharing, since
  folders are recreated as new.  Files will have the "archive" attribute set;
  they won't lose other attributes or metadata.

  Apache License or GNU General Public License
  --------------------------------------------
  SortFatFolder2 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.

  Graphical Versus Console Application
  ------------------------------------
  The Java command line may contain options or folder names.  If no folder
  names are given on the command line, then this program runs as a graphical or
  "GUI" application with the usual dialog boxes and windows.  See the "-?"
  option for a help summary:

      java  SortFatFolder2  -?

  The command line has more options than are visible in the graphical
  interface.  An option such as -u14 or -u16 is recommended because the default
  Java font is too small.  If folder names are given on the command line, then
  this program runs as a console application without a graphical interface.  A
  generated report is written on standard output, and may be redirected with
  the ">" or "1>" operators.  (Standard error may be redirected with the "2>"
  operator.)  An example command line is:

      java  SortFatFolder2  -s  d:\temp  >report.txt

  The console application will return an exit status of 1 for success, -1 for
  failure, and 0 for unknown.
*/

import java.awt.*;                // older Java GUI support
import java.awt.event.*;          // older Java GUI event support
import java.io.*;                 // standard I/O
import java.text.*;               // number formatting
import java.util.*;               // calendars, dates, lists, maps, vectors
import java.util.regex.*;         // regular expressions
import javax.swing.*;             // newer Java GUI support
import javax.swing.border.*;      // decorative borders

public class SortFatFolder2
{
  /* constants */

  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2017 by Keith Fenske.  Apache License or GNU GPL.";
  static final int DEFAULT_HEIGHT = -1; // default window height in pixels
  static final int DEFAULT_LEFT = 50; // default window left position ("x")
  static final int DEFAULT_TOP = 50; // default window top position ("y")
  static final int DEFAULT_WIDTH = -1; // default window width in pixels
  static final String EMPTY_STATUS = ""; // message when no status to display
  static final int EXIT_FAILURE = -1; // incorrect request or errors found
  static final int EXIT_SUCCESS = 1; // request completed successfully
  static final int EXIT_UNKNOWN = 0; // don't know or nothing really done
  static final String[] FONT_SIZES = {"10", "12", "14", "16", "18", "20", "24",
    "30"};                        // point sizes for text in output text area
  static final int MIN_FRAME = 200; // minimum window height or width in pixels
  static final int ORDER_SUBFIRST = 0; // put all subfolders before files
  static final int ORDER_SUBLAST = 1; // put subfolders after all files
  static final int ORDER_SUBMIXED = 2; // mix files and subfolders by name
  static final String PROGRAM_TITLE =
    "Sort FAT16/FAT32 Folder in Alphabetical Order - by: Keith Fenske";
  static final String SYSTEM_FONT = "Dialog"; // this font is always available
  static final int TIMER_DELAY = 700; // 0.700 seconds between status updates
  static final int WAIT_CREATE = 20; // delay in ms before create subfolder
  static final int WAIT_DELETE = 50; // delay in ms before delete subfolder
  static final int WAIT_MOVE = 10; // delay in ms before move file, subfolder
  static final int WAIT_RENAME = 200; // delay in ms before replace main folder

  /* class variables */

  static JButton cancelButton;    // graphical button for <cancelFlag>
  static boolean cancelFlag;      // our signal from user to stop processing
  static JCheckBox caseCheckbox;  // graphical option for <caseFlag>
  static boolean caseFlag;        // true if upper/lower case names different
  static JButton exitButton;      // "Exit" button for ending this application
  static JFileChooser fileChooser; // asks for input and output file names
  static String fontName;         // font name for text in output text area
  static JComboBox fontNameDialog; // graphical option for <fontName>
  static int fontSize;            // point size for text in output text area
  static JComboBox fontSizeDialog; // graphical option for <fontSize>
  static NumberFormat formatComma; // formats with commas (digit grouping)
  static JFrame mainFrame;        // this application's GUI window
  static boolean mswinFlag;       // true if running on Microsoft Windows
  static JButton openButton;      // "Open" button for files or folders
  static File[] openFileList;     // list of files selected by user
  static Thread openFilesThread;  // separate thread for doOpenButton() method
  static JTextArea outputText;    // generated report while opening files
  static JCheckBox recurseCheckbox; // graphical option for <recurseFlag>
  static boolean recurseFlag;     // true if we search folders and subfolders
  static JButton saveButton;      // "Save" button for writing output text
  static int sortOrderIndex;      // sorting order for subfolders, files
  static String sortPrefixFile;   // sorting prefix for files
  static String sortPrefixFolder; // sorting prefix for subfolders
  static JLabel statusDialog;     // status message during extended processing
  static String statusPending;    // will become <statusDialog> after delay
  static javax.swing.Timer statusTimer; // timer for updating status message
  static JRadioButton subFirstButton, subLastButton, subMixedButton;
                                  // sort order for subfolders versus files
  static long totalMoved;         // total number of files, subfolders moved
  static long totalSorted;        // total number of subfolders (re)sorted

/*
  main() method

  If we are running as a GUI application, set the window layout and then let
  the graphical interface run the show.
*/
  public static void main(String[] args)
  {
    ActionListener action;        // our shared action listener
    Font buttonFont;              // font for buttons, labels, status, etc
    boolean consoleFlag;          // true if running as a console application
    Border emptyBorder;           // remove borders around text areas
    int i;                        // index variable
    boolean maximizeFlag;         // true if we maximize our main window
    int windowHeight, windowLeft, windowTop, windowWidth;
                                  // position and size for <mainFrame>
    String word;                  // one parameter from command line

    /* Initialize variables used by both console and GUI applications. */

    buttonFont = null;            // by default, don't use customized font
    cancelFlag = false;           // don't cancel unless user complains
    caseFlag = false;             // ignore uppercase/lowercase in file names
    consoleFlag = false;          // assume no files or folders on command line
    fontName = "Verdana";         // preferred font name for output text area
    fontSize = 16;                // default point size for output text area
    mainFrame = null;             // during setup, there is no GUI window
    maximizeFlag = false;         // by default, don't maximize our main window
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    recurseFlag = true;           // default for processing folders, subfolders
//  sortOrderIndex =              // see call to setSortOrder() below
//  sortPrefixFile =              // see call to setSortOrder() below
//  sortPrefixFolder =            // see call to setSortOrder() below
    statusPending = EMPTY_STATUS; // begin with no text for <statusDialog>
    totalMoved = totalSorted = 0; // reset all global file counters
    windowHeight = DEFAULT_HEIGHT; // default window position and size
    windowLeft = DEFAULT_LEFT;
    windowTop = DEFAULT_TOP;
    windowWidth = DEFAULT_WIDTH;

    /* Initialize number formatting styles. */

    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups

    /* Initialize sorting order for subfolders versus files.  This sets global
    variables <sortOrderIndex>, <sortPrefixFile>, and <sortPrefixFolder>. */

    setSortOrder(ORDER_SUBFIRST); // set sorting order for subfolders, files

    /* Check command-line parameters for options. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore empty parameters, which are more common than you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
        || word.equals("-h") || (mswinFlag && word.equals("/h"))
        || word.equals("-help") || (mswinFlag && word.equals("/help")))
      {
        showHelp();               // show help summary
        System.exit(EXIT_UNKNOWN); // exit application after printing help
      }

      else if (word.equals("-c") || (mswinFlag && word.equals("/c"))
        || word.equals("-c1") || (mswinFlag && word.equals("/c1")))
      {
        caseFlag = true;          // uppercase/lowercase distinct in file names
      }
      else if (word.equals("-c0") || (mswinFlag && word.equals("/c0")))
      {
        caseFlag = false;         // ignore uppercase/lowercase in file names
      }

      else if (word.equals("-f0") || (mswinFlag && word.equals("/f0")))
      {
        setSortOrder(ORDER_SUBFIRST); // put all subfolders before files
      }
      else if (word.equals("-f1") || (mswinFlag && word.equals("/f1")))
      {
        setSortOrder(ORDER_SUBLAST); // put subfolders after all files
      }
      else if (word.equals("-f2") || (mswinFlag && word.equals("/f2")))
      {
        setSortOrder(ORDER_SUBMIXED); // mix files and subfolders by name
      }

      else if (word.equals("-s") || (mswinFlag && word.equals("/s"))
        || word.equals("-s1") || (mswinFlag && word.equals("/s1")))
      {
        recurseFlag = true;       // start doing subfolders
      }
      else if (word.equals("-s0") || (mswinFlag && word.equals("/s0")))
        recurseFlag = false;      // stop doing subfolders

      else if (word.startsWith("-u") || (mswinFlag && word.startsWith("/u")))
      {
        /* This option is followed by a font point size that will be used for
        buttons, dialogs, labels, etc. */

        int size = -1;            // default value for font point size
        try                       // try to parse remainder as unsigned integer
        {
          size = Integer.parseInt(word.substring(2));
        }
        catch (NumberFormatException nfe) // if not a number or bad syntax
        {
          size = -1;              // set result to an illegal value
        }
        if ((size < 10) || (size > 99))
        {
          System.err.println("Dialog font size must be from 10 to 99: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
        buttonFont = new Font(SYSTEM_FONT, Font.PLAIN, size); // for big sizes
//      buttonFont = new Font(SYSTEM_FONT, Font.BOLD, size); // for small sizes
        fontSize = size;          // use same point size for output text font
      }

      else if (word.startsWith("-w") || (mswinFlag && word.startsWith("/w")))
      {
        /* This option is followed by a list of four numbers for the initial
        window position and size.  All values are accepted, but small heights
        or widths will later force the minimum packed size for the layout. */

        Pattern pattern = Pattern.compile(
          "\\s*\\(\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*,\\s*(\\d{1,5})\\s*\\)\\s*");
        Matcher matcher = pattern.matcher(word.substring(2)); // parse option
        if (matcher.matches())    // if option has proper syntax
        {
          windowLeft = Integer.parseInt(matcher.group(1));
          windowTop = Integer.parseInt(matcher.group(2));
          windowWidth = Integer.parseInt(matcher.group(3));
          windowHeight = Integer.parseInt(matcher.group(4));
        }
        else                      // bad syntax or too many digits
        {
          System.err.println("Invalid window position or size: " + args[i]);
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      else if (word.equals("-x") || (mswinFlag && word.equals("/x")))
        maximizeFlag = true;      // true if we maximize our main window

      else if (word.startsWith("-") || (mswinFlag && word.startsWith("/")))
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }

      else
      {
        /* Parameter does not look like an option.  Assume this is a file or
        folder name. */

        consoleFlag = true;       // don't allow GUI methods to be called
        processFileOrFolder(new File(args[i]));
        if (cancelFlag) break;    // exit <for> loop if cancel or fatal error
      }
    }

    /* If running as a console application, print a summary of what we found
    and/or changed.  Exit to the system with an integer status. */

    if (consoleFlag)              // was at least one file/folder given?
    {
      printSummary();             // what we found and what was changed
      if (cancelFlag)             // were any fatal errors found?
        System.exit(EXIT_FAILURE);
      else if ((totalMoved > 0) || (totalSorted > 0)) // did we find anything?
        System.exit(EXIT_SUCCESS);
      else                        // if there were no files at all
        System.exit(EXIT_UNKNOWN);
    }

    /* There were no file or folder names on the command line.  Open the
    graphical user interface (GUI).  We don't need to be inside an if-then-else
    construct here because the console application called System.exit() above.
    The standard Java interface style is the most reliable, but you can switch
    to something closer to the local system, if you want. */

//  try
//  {
//    UIManager.setLookAndFeel(
//      UIManager.getCrossPlatformLookAndFeelClassName());
////    UIManager.getSystemLookAndFeelClassName());
//  }
//  catch (Exception ulafe)
//  {
//    System.err.println("Unsupported Java look-and-feel: " + ulafe);
//  }

    /* Initialize shared graphical objects. */

    action = new SortFatFolder2User(); // create our shared action listener
    emptyBorder = BorderFactory.createEmptyBorder(); // for removing borders
    fileChooser = new JFileChooser(); // create our shared file chooser
    statusTimer = new javax.swing.Timer(TIMER_DELAY, action);
                                  // update status message on clock ticks only

    /* If our preferred font is not available for the output text area, then
    use the boring default font for the local system. */

    if (fontName.equals((new Font(fontName, Font.PLAIN, fontSize)).getFamily())
      == false)                   // create font, read back created name
    {
      fontName = SYSTEM_FONT;     // must replace with standard system font
    }

    /* Create the graphical interface as a series of little panels inside
    bigger panels.  The intermediate panel names are of no lasting importance
    and hence are only numbered (panel01, panel02, etc). */

    /* Create a vertical box to stack buttons and options. */

    JPanel panel01 = new JPanel();
    panel01.setLayout(new BoxLayout(panel01, BoxLayout.Y_AXIS));

    /* Create a horizontal panel for the action buttons. */

    JPanel panel11 = new JPanel(new BorderLayout(0, 0));

    openButton = new JButton("Open Folder...");
    openButton.addActionListener(action);
    if (buttonFont != null) openButton.setFont(buttonFont);
    openButton.setMnemonic(KeyEvent.VK_O);
    openButton.setToolTipText("Find folders, move files.");
    panel11.add(openButton, BorderLayout.WEST);

    JPanel panel12 = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));

    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(action);
    cancelButton.setEnabled(false);
    if (buttonFont != null) cancelButton.setFont(buttonFont);
    cancelButton.setMnemonic(KeyEvent.VK_C);
    cancelButton.setToolTipText("Stop finding, moving files.");
    panel12.add(cancelButton);

    saveButton = new JButton("Save Output...");
    saveButton.addActionListener(action);
    if (buttonFont != null) saveButton.setFont(buttonFont);
    saveButton.setMnemonic(KeyEvent.VK_S);
    saveButton.setToolTipText("Copy output text to a file.");
    panel12.add(saveButton);

    panel11.add(panel12, BorderLayout.CENTER);

    exitButton = new JButton("Exit");
    exitButton.addActionListener(action);
    if (buttonFont != null) exitButton.setFont(buttonFont);
    exitButton.setMnemonic(KeyEvent.VK_X);
    exitButton.setToolTipText("Close this program.");
    panel11.add(exitButton, BorderLayout.EAST);

    panel01.add(panel11);
    panel01.add(Box.createVerticalStrut(10)); // space between panels

    /* Options for the sorting order of subfolders versus files. */

    JPanel panel21 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    ButtonGroup group22 = new ButtonGroup();

    subFirstButton = new JRadioButton("Put subfolders before files, ",
      (sortOrderIndex == ORDER_SUBFIRST));
    if (buttonFont != null) subFirstButton.setFont(buttonFont);
    subFirstButton.addActionListener(action); // do last so don't fire early
    group22.add(subFirstButton);
    panel21.add(subFirstButton);

    subLastButton = new JRadioButton("files before subfolders, or ",
      (sortOrderIndex == ORDER_SUBLAST));
    if (buttonFont != null) subLastButton.setFont(buttonFont);
    subLastButton.addActionListener(action); // do last so don't fire early
    group22.add(subLastButton);
    panel21.add(subLastButton);

    subMixedButton = new JRadioButton("mix files with subfolders.",
      (sortOrderIndex == ORDER_SUBMIXED));
    if (buttonFont != null) subMixedButton.setFont(buttonFont);
    subMixedButton.addActionListener(action); // do last so don't fire early
    group22.add(subMixedButton);
    panel21.add(subMixedButton);

    panel01.add(panel21);
    panel01.add(Box.createVerticalStrut(10)); // space between panels

    /* Miscellaneous options. */

    JPanel panel31 = new JPanel(new BorderLayout(10, 0));

    JPanel panel32 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    fontNameDialog = new JComboBox(GraphicsEnvironment
      .getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    fontNameDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) fontNameDialog.setFont(buttonFont);
    fontNameDialog.setSelectedItem(fontName); // select default font name
    fontNameDialog.setToolTipText("Font name for output text.");
    fontNameDialog.addActionListener(action); // do last so don't fire early
    panel32.add(fontNameDialog);

    panel32.add(Box.createHorizontalStrut(5));

    TreeSet sizelist = new TreeSet(); // collect font sizes 10 to 99 in order
    word = String.valueOf(fontSize); // convert number to a string we can use
    sizelist.add(word);           // add default or user's chosen font size
    for (i = 0; i < FONT_SIZES.length; i ++) // add our preferred size list
      sizelist.add(FONT_SIZES[i]); // assume sizes are all two digits (10-99)
    fontSizeDialog = new JComboBox(sizelist.toArray()); // give user nice list
    fontSizeDialog.setEditable(false); // user must select one of our choices
    if (buttonFont != null) fontSizeDialog.setFont(buttonFont);
    fontSizeDialog.setSelectedItem(word); // selected item is our default size
    fontSizeDialog.setToolTipText("Point size for output text.");
    fontSizeDialog.addActionListener(action); // do last so don't fire early
    panel32.add(fontSizeDialog);

    panel31.add(panel32, BorderLayout.WEST);

    JPanel panel33 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

    recurseCheckbox = new JCheckBox("sort subfolders", recurseFlag);
    if (buttonFont != null) recurseCheckbox.setFont(buttonFont);
    recurseCheckbox.setToolTipText("Select to sort folders and subfolders.");
    recurseCheckbox.addActionListener(action); // do last so don't fire early
    panel33.add(recurseCheckbox);

    panel33.add(Box.createHorizontalStrut(10));

    caseCheckbox = new JCheckBox("strict case in file names", caseFlag);
    if (buttonFont != null) caseCheckbox.setFont(buttonFont);
    caseCheckbox.setToolTipText(
      "Select if uppercase, lowercase different in file names.");
    caseCheckbox.addActionListener(action); // do last so don't fire early
    panel33.add(caseCheckbox);

    panel31.add(panel33, BorderLayout.EAST);

    panel01.add(panel31);
    panel01.add(Box.createVerticalStrut(15)); // space between panels

    /* Bind all of the buttons and options above into a single panel so that
    the layout does not change when the window changes. */

    JPanel panel41 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
    panel41.add(panel01);

    /* Create a scrolling text area for the generated output. */

    outputText = new JTextArea(20, 40);
    outputText.setEditable(false); // user can't change this text area
    outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    outputText.setLineWrap(false); // don't wrap text lines
    outputText.setMargin(new Insets(5, 6, 5, 6)); // top, left, bottom, right
    outputText.setText(
      "\nSort the directory entries for a FAT16 or FAT32 file folder.  Unlike"
      + "\nnewer file systems (NTFS), the FAT file system does not keep"
      + "\ndirectories in alphabetical order."
      + "\n\nDO NOT USE THIS PROGRAM ON SYSTEM FOLDERS.  The only safe"
      + "\nfolders are those you create with your own files and no hidden or"
      + "\nsystem files.  The program has no purpose on any file system that"
      + "\nmaintains an internal sorted order."
      + "\n\nCopyright (c) 2017 by Keith Fenske.  By using this program, you"
      + "\nagree to terms and conditions of the Apache License and/or GNU"
      + "\nGeneral Public License.\n\n");

    JScrollPane panel51 = new JScrollPane(outputText);
    panel51.setBorder(emptyBorder); // no border necessary here

    /* Create an entire panel just for the status message.  Set margins with a
    BorderLayout, because a few pixels higher or lower can make a difference in
    whether the position of the status text looks correct. */

    statusDialog = new JLabel(statusPending, JLabel.RIGHT);
    if (buttonFont != null) statusDialog.setFont(buttonFont);

    JPanel panel61 = new JPanel(new BorderLayout(0, 0));
    panel61.add(Box.createVerticalStrut(7), BorderLayout.NORTH);
    panel61.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    panel61.add(statusDialog, BorderLayout.CENTER);
    panel61.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
//  panel61.add(Box.createVerticalStrut(5), BorderLayout.SOUTH);

    /* Combine buttons and options with output text.  The text area expands and
    contracts with the window size.  Put our status message at the bottom. */

    JPanel panel71 = new JPanel(new BorderLayout(0, 0));
    panel71.add(panel41, BorderLayout.NORTH); // buttons and options
    panel71.add(panel51, BorderLayout.CENTER); // text area
    panel71.add(panel61, BorderLayout.SOUTH); // status message

    /* Create the main window frame for this application.  We supply our own
    margins using the edges of the frame's border layout. */

    mainFrame = new JFrame(PROGRAM_TITLE);
    Container panel72 = mainFrame.getContentPane(); // where content meets frame
    panel72.setLayout(new BorderLayout(0, 0));
    panel72.add(Box.createVerticalStrut(15), BorderLayout.NORTH); // top margin
    panel72.add(Box.createHorizontalStrut(5), BorderLayout.WEST); // left
    panel72.add(panel71, BorderLayout.CENTER); // actual content in center
    panel72.add(Box.createHorizontalStrut(5), BorderLayout.EAST); // right
    panel72.add(Box.createVerticalStrut(5), BorderLayout.SOUTH); // bottom

    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setLocation(windowLeft, windowTop); // normal top-left corner
    if ((windowHeight < MIN_FRAME) || (windowWidth < MIN_FRAME))
      mainFrame.pack();           // do component layout with minimum size
    else                          // the user has given us a window size
      mainFrame.setSize(windowWidth, windowHeight); // size of normal window
    if (maximizeFlag) mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    mainFrame.validate();         // recheck application window layout
    mainFrame.setVisible(true);   // and then show application window

    /* Let the graphical interface run the application now. */

    openButton.requestFocusInWindow(); // give keyboard focus to "Open" button

  } // end of main() method

// ------------------------------------------------------------------------- //

/*
  doCancelButton() method

  This method is called while we are opening files or folders if the user wants
  to end the processing early, perhaps because it is taking too long.  We must
  cleanly terminate any secondary threads.  Leave whatever output has already
  been generated in the output text area.
*/
  static void doCancelButton()
  {
    if (JOptionPane.showConfirmDialog(mainFrame,
      "If you quit now, some files will be in the original folder and\nsome in a temporary folder. This could be a problem.\n\nDo you really want to cancel?",
      "Please Confirm Cancel", JOptionPane.YES_NO_OPTION,
      JOptionPane.PLAIN_MESSAGE) != JOptionPane.YES_OPTION)
    {
      return;                     // user cancelled the cancel, go back to work
    }
    cancelFlag = true;            // tell other threads that all work stops now
    putOutput("Cancelled by user."); // print message and scroll
  }


/*
  doOpenButton() method

  Allow the user to select one or more files or folders for processing.
*/
  static void doOpenButton()
  {
    /* Ask the user for input files or folders. */

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
//  fileChooser.setDialogTitle("Open Files or Folders...");
    fileChooser.setDialogTitle("Select Folder or Folders...");
    fileChooser.setFileHidingEnabled(true); // don't show hidden files
//  fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setMultiSelectionEnabled(true); // allow more than one file
    if (fileChooser.showOpenDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
//  openFileList = sortFileList(fileChooser.getSelectedFiles());
                                  // get list of files selected by user
    openFileList = fileChooser.getSelectedFiles();
                                  // no sorting, same order as chosen by user

    /* We have a list of files or folders.  Disable the "Open" button until we
    are done, and enable a "Cancel" button in case our secondary thread runs
    for a long time and the user panics. */

    cancelButton.setEnabled(true); // enable button to cancel this processing
    cancelFlag = false;           // but don't cancel unless user complains
    openButton.setEnabled(false); // suspend "Open" button until we are done
    outputText.setText("");       // clear output text area
    totalMoved = totalSorted = 0; // reset all global file counters

    setStatusMessage(EMPTY_STATUS); // clear text in status message
    statusTimer.start();          // start updating status on clock ticks

    openFilesThread = new Thread(new SortFatFolder2User(), "doOpenRunner");
    openFilesThread.setPriority(Thread.MIN_PRIORITY);
                                  // use low priority for heavy-duty workers
    openFilesThread.start();      // run separate thread to open files, report

  } // end of doOpenButton() method


/*
  doOpenRunner() method

  This method is called inside a separate thread by the runnable interface of
  our "user" class to process the user's selected files in the context of the
  "main" class.  By doing all the heavy-duty work in a separate thread, we
  won't stall the main thread that runs the graphical interface, and we allow
  the user to cancel the processing if it takes too long.
*/
  static void doOpenRunner()
  {
    int i;                        // index variable

    /* Loop once for each file name selected.  Don't assume that these are all
    valid file names. */

    for (i = 0; i < openFileList.length; i ++)
    {
      if (cancelFlag) break;      // exit <for> loop if cancel or fatal error
      processFileOrFolder(openFileList[i]); // process this file or folder
    }

    /* Print a summary and scroll the output, even if we were cancelled. */

    printSummary();               // what we found and what was changed

    /* We are done.  Turn off the "Cancel" button and allow the user to click
    the "Start" button again. */

    cancelButton.setEnabled(false); // disable "Cancel" button
    openButton.setEnabled(true);  // enable "Open" button

    statusTimer.stop();           // stop updating status message by timer
    setStatusMessage(EMPTY_STATUS); // and clear any previous status message

  } // end of doOpenRunner() method


/*
  doSaveButton() method

  Ask the user for an output file name, create or replace that file, and copy
  the contents of our output text area to that file.  The output file will be
  in the default character set for the system, so if there are special Unicode
  characters in the displayed text (Arabic, Chinese, Eastern European, etc),
  then you are better off copying and pasting the output text directly into a
  Unicode-aware application like Microsoft Word.
*/
  static void doSaveButton()
  {
    FileWriter output;            // output file stream
    File userFile;                // file chosen by the user

    /* Ask the user for an output file name. */

    fileChooser.resetChoosableFileFilters(); // remove any existing filters
    fileChooser.setDialogTitle("Save Output as Text File...");
    fileChooser.setFileHidingEnabled(true); // don't show hidden files
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false); // allow only one file
    if (fileChooser.showSaveDialog(mainFrame) != JFileChooser.APPROVE_OPTION)
      return;                     // user cancelled file selection dialog box
    userFile = fileChooser.getSelectedFile();

    /* See if we can write to the user's chosen file. */

    if (userFile.isDirectory())   // can't write to directories or folders
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is a directory or folder.\nPlease select a normal file."));
      return;
    }
    else if (userFile.isHidden()) // won't write to hidden (protected) files
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is a hidden or protected file.\nPlease select a normal file."));
      return;
    }
    else if (userFile.isFile() == false) // if file doesn't exist
    {
      /* Maybe we can create a new file by this name.  Do nothing here. */
    }
    else if (userFile.canWrite() == false) // file exists, but is read-only
    {
      JOptionPane.showMessageDialog(mainFrame, (userFile.getName()
        + " is locked or write protected.\nCan't write to this file."));
      return;
    }
    else if (JOptionPane.showConfirmDialog(mainFrame, (userFile.getName()
      + " already exists.\nDo you want to replace this with a new file?"))
      != JOptionPane.YES_OPTION)
    {
      return;                     // user cancelled file replacement dialog
    }

    /* Write lines to output file. */

    try                           // catch file I/O errors
    {
      output = new FileWriter(userFile); // try to open output file
      outputText.write(output);   // couldn't be much easier for writing!
      output.close();             // try to close output file
    }
    catch (IOException ioe)
    {
      putOutput("Can't write to text file: " + ioe.getMessage());
    }
  } // end of doSaveButton() method


/*
  moveFiles() method

  Move all files and subfolders from an original folder to a new folder.  We do
  this in Java by renaming; there is no explicit "move" method.
*/
  static void moveFiles(File oldFolder, File newFolder)
  {
    File[] contents;              // sorted contents of <oldFolder>
    int i;                        // index variable
    File next;                    // next File object from <contents>
    long stamp;                   // date and time stamp for file
    File target;                  // where next File object goes

    if (cancelFlag) return;       // stop if user cancel or fatal error
    contents = sortFileList(oldFolder.listFiles()); // no filter, but sorted
    for (i = 0; i < contents.length; i ++) // for each file in order
    {
      if (cancelFlag) return;     // stop if user cancel or fatal error
      next = contents[i];         // get next File object from <contents>
      if (next.exists() == false) continue; // ignore missing files, folders
      setStatusMessage(next.getPath()); // running status is path + file name
      stamp = next.lastModified(); // save current date and time stamp
      target = new File(newFolder, next.getName()); // where to rename

      if (recurseFlag && next.isDirectory()) // recursive subfolder?
      {
        putOutput("Resorting subfolder: " + next.getPath());
        waitForSystem(WAIT_CREATE); // delay before create subfolder
        if (target.mkdir() == false) // create copy of this subfolder
        {
          putOutput("Can't create subfolder: " + target.getPath());
          cancelFlag = true;      // don't do anything more
          return;
        }
        moveFiles(next, target);  // move files and subfolders

        if (cancelFlag) return;   // stop if user cancel or fatal error

        waitForSystem(WAIT_DELETE); // delay before delete subfolder
        if (next.delete() == false) // delete original folder, now empty
        {
          putOutput("Can't delete subfolder: " + next.getPath());
          cancelFlag = true;
          return;
        }
        totalSorted ++;           // one more recursive subfolder done
      }

      else                        // file or subfolder to be moved
      {
        waitForSystem(WAIT_MOVE); // delay before move file, subfolder
        if (next.renameTo(target) == false) // try to move by renaming
        {
          putOutput("Can't rename " + next.getPath() + " as "
            + target.getPath());
          cancelFlag = true;
          return;
        }
        totalMoved ++;            // one more file or subfolder moved
      }

      target.setLastModified(stamp); // date time stamp, ignore errors
    }
  } // end of moveFiles() method


/*
  printSummary() method

  Tell the user what we found and what was changed.
*/
  static void printSummary()
  {
    if (cancelFlag)               // if cancelled by user or fatal error
    {
      putOutput("After an error, files may be in their original folder or a temporary folder.");
    }
    putOutput("Moved " + formatComma.format(totalMoved)
      + ((totalMoved == 1) ? " file (or subfolder)" : " files (or subfolders)")
      + " and sorted " + formatComma.format(totalSorted)
      + ((totalSorted == 1) ? " subfolder." : " subfolders."));
  }


/*
  processFileOrFolder() method

  The caller gives us a Java File object that may be a file, a folder, or just
  random garbage.  We only do whole folders (directories) in this program, not
  individual files.
*/
  static void processFileOrFolder(File givenFile)
  {
    long stamp;                   // date and time stamp for file
    File startFolder;             // user's given folder
    File startParent;             // parent folder of given folder
    File startTemp;               // temporary then new folder

    if (cancelFlag) return;       // stop if user cancel or fatal error

    /* First we need the real (canonical) name of the folder.  Then we need the
    parent folder, so we can create and rename a new folder. */

    startFolder = givenFile;      // look closely at user's folder
    try { startFolder = startFolder.getCanonicalFile(); }
    catch (IOException ioe) { startFolder = null; }
    if ((startFolder == null) || (startFolder.isDirectory() == false))
    {
      putOutput("Not a folder (directory): " + givenFile.getPath());
      cancelFlag = true;          // don't do anything more
      return;
    }
    stamp = startFolder.lastModified(); // save current date and time stamp
    putOutput("Original folder is: " + startFolder.getPath());

    startParent = startFolder.getParentFile(); // also need parent folder
    if (startParent == null)      // happens if given root folder on a drive
    {
      putOutput("Can't get parent folder for: " + startFolder.getPath());
      cancelFlag = true;
      return;
    }
    putOutput("Parent folder is: " + startParent.getPath());

    startTemp = new File(startParent, "Temp" + System.currentTimeMillis());
//  waitForSystem(WAIT_CREATE);   // delay before create folder
    if (startTemp.mkdir() == false) // we need a new and temporary folder
    {
      putOutput("Can't create temporary folder: " + startTemp.getPath());
      cancelFlag = true;
      return;
    }
    putOutput("Temporary folder is: " + startTemp.getPath());
    if (cancelFlag) return;       // stop if user cancel or fatal error

    /* Call a recursive subroutine to move all files and subfolders from the
    user's given folder to our new temporary folder. */

    moveFiles(startFolder, startTemp); // move files and subfolders
    if (cancelFlag) return;       // stop if user cancel or fatal error

    /* Delete the folder given by the user, and rename our temporary folder to
    have the original name. */

    waitForSystem(WAIT_DELETE);   // delay before delete original folder
    if (startFolder.delete() == false) // delete original folder, now empty
    {
      putOutput("Can't delete original folder: " + startFolder.getPath());
      cancelFlag = true;
      return;
    }

    waitForSystem(WAIT_RENAME);   // delay before replace original folder
    if (startTemp.renameTo(startFolder) == false) // temporary becomes original
    {
      putOutput("Can't rename " + startTemp.getPath() + " as "
        + startFolder.getPath());
      cancelFlag = true;
      return;
    }

    startFolder.setLastModified(stamp); // date time stamp, ignore errors

  } // end of processFileOrFolder() method


/*
  putOutput() method

  Append a complete line of text to the end of the output text area.  We add a
  newline character at the end of the line, not the caller.  By forcing all
  output to go through this same method, one complete line at a time, the
  generated output is cleaner and can be redirected.

  The output text area is forced to scroll to the end, after the text line is
  written, by selecting character positions that are much too large (and which
  are allowed by the definition of the JTextComponent.select() method).  This
  is easier and faster than manipulating the scroll bars directly.  However, it
  does cancel any selection that the user might have made, for example, to copy
  text from the output area.
*/
  static void putOutput(String text)
  {
    if (mainFrame == null)        // during setup, there is no GUI window
      System.out.println(text);   // console output goes onto standard output
    else
    {
      outputText.append(text + "\n"); // graphical output goes into text area
      outputText.select(999999999, 999999999); // force scroll to end of text
    }
  }


/*
  setSortOrder() method

  Several global variables must be set to ensure the correct sorting order when
  folders contain both files and subfolders.
*/
  static void setSortOrder(int index)
  {
    switch (index)
    {
      case (ORDER_SUBFIRST):      // put all subfolders before files
        sortOrderIndex = index;
        sortPrefixFile = "2 ";
        sortPrefixFolder = "1 ";
        break;

      case (ORDER_SUBLAST):       // put subfolders after all files
        sortOrderIndex = index;
        sortPrefixFile = "1 ";
        sortPrefixFolder = "2 ";
        break;

      case (ORDER_SUBMIXED):      // mix files and subfolders by name
        sortOrderIndex = index;
        sortPrefixFile = sortPrefixFolder = "";
        break;

      default:
        System.err.println("Error in setSortOrder(): unknown index = "
          + index);               // should never happen, so write on console
        break;
    }
  } // end of setSortOrder() method


/*
  setStatusMessage() method

  Set the text for the status message if we are running as a GUI application.
  This gives the user some indication of our progress when processing is slow.
  If the update timer is running, then this message will not appear until the
  timer kicks in.  This prevents the status from being updated too often, and
  hence being unreadable.
*/
  static void setStatusMessage(String text)
  {
    if (mainFrame == null)        // are we running as a console application?
      return;                     // yes, console doesn't show running status
    statusPending = text;         // always save caller's status message
    if (statusTimer.isRunning())  // are we updating on a timed basis?
      return;                     // yes, wait for the timer to do an update
    statusDialog.setText(statusPending); // show the status message now
  }


/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("  java  SortFatFolder2  [options]  [folderNames]");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -c0 = ignore uppercase/lowercase in file names (default)");
    System.err.println("  -c1 = -c = strict Unicode order for case in file names");
    System.err.println("  -f0 = put subfolders before files in each directory (default)");
    System.err.println("  -f1 = put subfolders after files");
    System.err.println("  -f2 = mix files and subfolders by name only");
    System.err.println("  -s0 = process selected folders only, no subfolders");
    System.err.println("  -s1 = -s = process folders and subfolders (default)");
    System.err.println("  -u# = font size for buttons, dialogs, etc; default is local system;");
    System.err.println("      example: -u16");
    System.err.println("  -w(#,#,#,#) = normal window position: left, top, width, height;");
    System.err.println("      example: -w(50,50,700,500)");
    System.err.println("  -x = maximize application window; default is normal window");
    System.err.println();
    System.err.println("Output may be redirected with the \">\" operator.  If no folder names are given");
    System.err.println("on the command line, then a graphical interface will open.");
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method


/*
  sortFileList() method

  When we ask for a list of files or subfolders in a directory, the list is not
  likely to be in our preferred order.  Java does not guarantee any particular
  order, and the observed order is whatever is supplied by the underlying file
  system (which can be very jumbled for FAT16/FAT32).  We would like the file
  names to be sorted, and since we recurse on subfolders, we also want the
  subfolders to appear in order.

  The caller's parameter may be <null> and this may happen if the caller asks
  File.listFiles() for the contents of a protected system directory.  All calls
  to listFiles() in this program are wrapped inside a call to us, so we replace
  a null parameter with an empty array as our result.
*/
  static File[] sortFileList(File[] input)
  {
    String fileName;              // file name without the path
    int i;                        // index variable
    TreeMap list;                 // our list of files
    File[] result;                // our result
    StringBuffer sortKey;         // created sorting key for each file

    if (input == null)            // were we given a null pointer?
      result = new File[0];       // yes, replace with an empty array
    else if (input.length < 2)    // don't sort lists with zero or one element
      result = input;             // just copy input array as result array
    else
    {
      /* First, create a sorted list with our choice of index keys and the File
      objects as data.  Names are sorted as files or folders, then in lowercase
      to ignore differences in uppercase versus lowercase, then in the original
      form for systems where case is distinct. */

      list = new TreeMap();       // create empty sorted list with keys
      sortKey = new StringBuffer(); // allocate empty string buffer for keys
      for (i = 0; i < input.length; i ++)
      {
        sortKey.setLength(0);     // empty any previous contents of buffer
        sortKey.append(input[i].isDirectory() ? sortPrefixFolder
          : sortPrefixFile);      // put files before/after subfolders
        fileName = input[i].getName(); // get the file name without the path
        if (caseFlag == false)    // do we ignore uppercase versus lowercase?
        {
          sortKey.append(fileName.toLowerCase()); // start with same case
          sortKey.append(" ");    // separate lowercase from original case
        }
        sortKey.append(fileName); // then sort file name on original case
        list.put(sortKey.toString(), input[i]); // put file into sorted list
      }

      /* Second, now that the TreeMap object has done all the hard work of
      sorting, pull the File objects from the list in order as determined by
      the sort keys that we created. */

      result = (File[]) list.values().toArray(new File[0]);
    }
    return(result);               // give caller whatever we could find

  } // end of sortFileList() method


/*
  userButton() method

  This method is called by our action listener actionPerformed() to process
  buttons, in the context of the main SortFatFolder2 class.
*/
  static void userButton(ActionEvent event)
  {
    Object source = event.getSource(); // where the event came from
    if (source == cancelButton)   // "Cancel" button
    {
      doCancelButton();           // stop opening files or folders
    }
    else if (source == caseCheckbox) // uppercase/lowercase in file names
    {
      caseFlag = caseCheckbox.isSelected();
    }
    else if (source == exitButton) // "Exit" button
    {
      System.exit(0);             // always exit with zero status from GUI
    }
    else if (source == fontNameDialog) // font name for output text area
    {
      /* We can safely assume that the font name is valid, because we obtained
      the names from getAvailableFontFamilyNames(), and the user can't edit
      this dialog field. */

      fontName = (String) fontNameDialog.getSelectedItem();
      outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    }
    else if (source == fontSizeDialog) // point size for output text area
    {
      /* We can safely parse the point size as an integer, because we supply
      the only choices allowed, and the user can't edit this dialog field. */

      fontSize = Integer.parseInt((String) fontSizeDialog.getSelectedItem());
      outputText.setFont(new Font(fontName, Font.PLAIN, fontSize));
    }
    else if (source == openButton) // "Open" button for files or folders
    {
      doOpenButton();             // open files or folders for processing
    }
    else if (source == recurseCheckbox) // if we search folders and subfolders
    {
      recurseFlag = recurseCheckbox.isSelected();
    }
    else if (source == saveButton) // "Save Output" button
    {
      doSaveButton();             // write output text area to a file
    }
    else if (source == statusTimer) // update timer for status message text
    {
      if (statusPending.equals(statusDialog.getText()) == false)
        statusDialog.setText(statusPending); // new status, update the display
    }
    else if (source == subFirstButton) // radio button for file, subfolder order
    {
      setSortOrder(ORDER_SUBFIRST); // put all subfolders before files
    }
    else if (source == subLastButton) // radio button for file, subfolder order
    {
      setSortOrder(ORDER_SUBLAST); // put subfolders after all files
    }
    else if (source == subMixedButton) // radio button for file, subfolder order
    {
      setSortOrder(ORDER_SUBMIXED); // mix files and subfolders by name
    }
    else                          // fault in program logic, not by user
    {
      System.err.println("Error in userButton(): unknown ActionEvent: "
        + event);                 // should never happen, so write on console
    }
  } // end of userButton() method


/*
  waitForSystem() method

  Wait for a short period of time, so the system can finish pending operations
  on the file directory, such as after deleting the original main folder and
  before renaming the temporary copy to be the main folder.  This shouldn't be
  necessary but sometimes helps (not always).

  The sleep() method is not precise, and often results in a longer delay than
  what was requested.  For example, a request for 10 ms produces a delay of
  about 15 ms in Java 1.4 to 7 on Windows XP/7.  Some smaller values can be
  more accurate in other situations, but for this program, values below 10 ms
  have less of an effect on performance than operating system overhead.

  The default delays (WAIT*) can be set to zero on well-behaved systems.  Any
  need for longer delays indicates unusual hardware or a problem with optional
  software (i.e., anti-virus) being called when files are moved or folders are
  created and deleted.  Temporarily turn off your anti-virus and try again.
*/
  static void waitForSystem(
    int delay)                    // wait time in milliseconds (ms)
  {
    if (delay > 0) try { Thread.sleep(delay); }
    catch (InterruptedException ie) { /* do nothing */ }
  }

} // end of SortFatFolder2 class

// ------------------------------------------------------------------------- //

/*
  SortFatFolder2User class

  This class listens to input from the user and passes back event parameters to
  a static method in the main class.
*/

class SortFatFolder2User implements ActionListener, Runnable
{
  /* empty constructor */

  public SortFatFolder2User() { }

  /* button listener, dialog boxes, etc */

  public void actionPerformed(ActionEvent event)
  {
    SortFatFolder2.userButton(event);
  }

  /* separate heavy-duty processing thread */

  public void run()
  {
    SortFatFolder2.doOpenRunner();
  }

} // end of SortFatFolder2User class

/* Copyright (c) 2017 by Keith Fenske.  Apache License or GNU GPL. */
