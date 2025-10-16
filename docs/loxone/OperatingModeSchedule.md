# Operating Mode Schedule

> **Note:** This document is automatically generated from the official Loxone PDF documentation.
> Last updated: 2025-10-16 11:06:00 UTC
> Source: https://www.loxone.com/wp-content/uploads/datasheets/OperatingModeSchedule.pdf

---

# **Operating Mode Schedule**

## 14.4


This feature allows users to get and modify a list of entries that trigger operating modes on
specific dates. New entries can be created and existing ones can be removed or modified.

### Table of contents


Table of contents

Entry

Calendar Mode & Calendar Mode Attributes

Administrative Commands

Commands

Revision History

### Entry

A json object containing all information on when a specific operating mode will be active. It
consists of the following attributes


  - uuid


    Identification, important for updating & deleting this entry.

  - name


    - A descriptive name for this entry.

  - operatingMode


    Identifies the operating mode that is being activated by this entry.

    - The names for these operating modes are contained in the Structure File.

  - calMode


    - See calendar mode for more details.


**14.4**


Operating Mode Schedule Page 2 of 4


### Calendar Mode & Calendar Mode Attributes

The calendar mode (0-5) and its calendar mode attributes specify when a specific entry and
therefore it’s operating mode will become active.


  - 0 = Yearly Date


    Specific date that is being repeated every year.

    - Attributes: “<startMonth>/<startDay>”

  - 1 = Easter


    - This entry will be repeated every year, but on a date that depends on what date
the easter sunday is on in that year.

    - Attributes: “<easterOffset>”


       - easterOffset


          - number of days before (< 0) or after (> 0) easter sunday.

          - E.g. 1 means one day after easter sunday = easter monday.

  2 = Specific Date


    This entry will only be active on one specific date.

    - Attributes: “<startYear>/<startMonth>/<startDay>”

  3 = Specific Timespan


    This entry will be active between two specific dates and will not be repeated.

    - Attributes:

“<startYear>/<startMonth>/<startDay>/<endYear>/<endMonth>/<endDay>”

  - 4 = Yearly Timespan

    - This entry will be active between two dates every year.

    - Attributes: “<startYear>/<startMonth>/<endYear>/<endMonth>”

  - 5 = Weekday


    This entry will be active repeatedly on specific weekdays in specific months. E.g.
the first monday in january.

    - Attributes: “<startMonth>/<weekDay>/<weekDayInMonth>”


       - startMonth


          specifies in what specific month this weekday is of interest.

          - 1 = January

          - ...

          - 12 = December

          - 13 = every month of the year

       - weekDay

          - 0 = monday

          - ..

          - 6 = sunday

       - weekDayInMonth

          - 0 = every occurrence of the weekday in the month

          1 = only on the first occurrence of the weekday in the month


**14.4**


Operating Mode Schedule Page 3 of 4


          - ...

          - 4 = fourth weekday of the month

          - 5 = only on the last occurrence of the weekday in the month

### Administrative Commands

These commands have a fundamental influence on how a smart home works. This is why they
can only be performed by users with administrative rights on the Miniserver.


  - jdev/sps/calendargetentries

    - Returns a JSON array containing all entries

  - jdev/sps/calendarcreateentry/<name>/<opMode>/<calMode>/<calModeAttr>

    - Creates a new entry, returns code 200 if successful.

  - jdev/sps/calendarupdateentry/<calUUID>/<name>/<opMode>/<calMode>/<calModeA

ttr>


    - Updates an existing entry, returns code 200 if successful.

  - jdev/sps/calendardeleteentry/<calUUID>

    - Deletes an existing entry, returns code 200 if successful.

### Commands


These commands can be performed by any user with valid credentials for the visualizations

of this Miniserver.


  - jdev/sps/calendargetheatperiod

    - Returns the current heating period on the Miniserver. It returns only the date
information of the heating period entry in a different format.

    - This command is used e.g. to

    - The return value comes as ISO-Date start / end, z.B. 10-15/04-15

  - jdev/sps/calendargetcoolperiod

    - Same as with the heating period.

### Revision History

#### **2023.11.2 - Initial Release**


**14.4**


Operating Mode Schedule Page 4 of 4


