

# Introduction #

After the Android Phones imports Contacts from SD card, the numbers will include "-", " ", "(" and ")".

Some application will not work normally if the number has the symbols above.

Contact Number fixer can help you remove these symbols from contact numbers.

Besides, some user hope it can remove the Contry Code and Area Code as well, this requirement had been implemented in the new version.

[Screenshots Screenshots are available in the Chinese Introduction](http://code.google.com/p/contact-number-fixer/wiki/CNFIntro?wl=zh-Hans)

# Update Log #

v1.1.2

  * Move Notificatio to Ongoing
  * Will not update via 2G network by default

v1.1.1

  * Normalize the font size of the Operation Message

v1.1.0

  * Support removing "(" and ")"  from contact numbers.

  * Support removing Country Code and Area Code.

  * Support background processing.

  * Increase the processing speed.

  * REMOVE ADVERSISEMENTS.

v1.0.5

  * Support removing the "-" and " " from contact numbers.

# About Removing Countroy Code and Area Code #

Because the rule of telephone number is too complex to remove without specifying some parameters such as Country Code, Area Code and Mobile Prefixes.

Take China for example. When removing the Country Code (86) from +862056789876 without removing the Area Code, it needs to prefix a Zero to the removed number. So the expected result is 02056789876, but the mobile number should not.

**Under the circumstances above, you must input the three parameter correctly to aviod unexpected process occurs.**


**I recommend you backup the contacts before removing Country Code and Area Codes.**


---


> [DONATE](https://dl.dropboxusercontent.com/u/1890357/donate/default.htm)