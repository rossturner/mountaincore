# Community Translations for Mountaincore

This is the official community translations mod for [_Mountaincore_](http://rocketjump.technology/mountaincore/)

## Description

As _Mountaincore_ is in active development, the text in the game is 
constantly being added to. Until release, it would not be feasible to produce 
a complete translation into the main languages as they would quickly fall out of date 
with each new release of the game.

The intention for this mod (and this git repository) is that the CSV files 
in the [translations](./translations) directory will be kept up to date with contributions from
the game's developers and community. 

## About the translation files

Each translation is in a separate CSV (comma separated values) file with 4 columns:

* KEY - The special code used by the game to load the correct text
* NOTES - Only provided for hints to translators
* ENGLISH - The original English translation for this KEY
* (language) - The translation of the English text into the relevant language

You should be able to open and edit these with any spreadsheet software such as 
Microsoft Excel or LibreOffice Calc. Be sure to save the file in its original format 
(.csv with commas `,` as the separator and double quotes `"` as the string delimiter). 
The character set is `UTF-8`.

There are already some existing translations from earlier rounds of supposedly professional 
localisation. Some of these did not have enough context and are poor translations, so feel 
free to correct and update anything you deem appropriate.

Anything between double curly-brackets {{LIKE THIS}} will be replaced with other text by the 
game engine and should not be changed as part of the translation. For example, the translation
of `Planting {{requiredItem}}` into German is `Pflanzt {{requiredItem}}`.

## How to Contribute

You can download and edit the relevant .csv file from [/translations](./translations)
and email your changes to ross@rocketjump.technology

Similarly if there is a language translation you would like to contribute to that does
not yet have a CSV file, please get in touch via [Discord](https://discord.gg/qF2S3tf) or email and it will be added.

Ideally, also please join the [game's Discord](https://discord.gg/qF2S3tf) and jump into the #translations channel.


## Version History

* 1.0.0
    * Initial Release
* 1.0.X
    * Added contributions to languages

## License

This project is licensed under the Creative Commons Legal Code License - see the [LICENSE.txt](./LICENSE.txt) file for details

## Contributors

### German translation
* Antafes
* Katharina Schrempf
* Ferdinand Niedermann
* Mario Röder

### French translation
* Rodolphe Moissinac

### Spanish translation
* Harlmorl

### Italian translation
* AndyD

### Danish translation
* Mathias kiær

### Turkish translation
* Fırat Gülmez

### Japanese translation
* SPQR

### Chinese (simplified) translation
* Howard Huang

