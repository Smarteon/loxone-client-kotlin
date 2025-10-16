# API Commands (Konektor)

> **Note:** This document is automatically generated from the official Loxone PDF documentation.
> Last updated: 2025-10-16 11:06:00 UTC
> Source: https://www.loxone.com/wp-content/uploads/datasheets/API-Commands.pdf

---

Date:



2022-06-30


# **API COMMANDS**

## **SET (Analog Input)**

Sends a defined value to an input of a function block.


**Syntax:**

SET( **FunctionBlock** ; **Input;Value** )

|Col1|Description|Comment|
|---|---|---|
|Function Block|Abbreviation of the function block to which the command<br>should be sent.||
|Input|Abbreviation of the input to which the command should be<br>sent.||
|Value|Value to be sent to the input of the function block. If the<br>field is left empty, the value is taken from the Touch Pure<br>Flex Button.||


## **SET (Digital Input)**


Sends a defined value to an input of a function block.


**Syntax:**

SET(FunctionBlock; Input; Value)


|Col1|Description|Comment|
|---|---|---|
|Function Block|Abbreviation of the function block to which the command<br>should be sent.||
|Input|Abbreviation of the input to which the command should be<br>sent.||
|Value|Value to be sent to the input of the function block. If the<br>field is left empty, the value is taken from the Touch Pure<br>Flex Button. Possible values:<br>  - pulse: Sends a pulse when pressing the button on the<br>Touch Pure Flex.||


Date:



2022-06-30




## **SETT5**

Set a specific button of a T5 input.


**Syntax:**

SETT5(FunctionBlock; Input; T5-Button;[Value])


|Col1|Description|Comment|
|---|---|---|
|Function Block|Abbreviation of the function block to which the<br>command should be sent.||
|Input|Abbreviation of the input to which the command<br>should be sent.||
|T5-Button|T5-button for which the value should be set. (1:<br>Shading up, 2: Volume up, 3: Light, 4: Shading down,<br>5: Volume down)||
|Value|Value to be sent to the input of the function block. If<br>the field is left empty, the value is taken from the<br>Touch Pure Flex Button. Possible values:<br>  - pulse: Sends a pulse when pressing the button on<br>the Touch Pure Flex.<br>  - On: Sends a 1 when pressing the button on the<br>Touch Pure Flex.<br> - Off: Sends a 0 when pressing the button on the<br>Touch Pure Flex.||


Date:



2022-06-30


## **MENU (Analog Input)____ Only relevant for the Touch Pure Flex.**

Implements a value selection via the up-down buttons on the Touch Pure Flex.

The user can select between different texts, after selecting a text the corresponding value is sent to the

function block.

The value-text pairs can be defined in the options of the command. If no value-text pairs are defined it is

tried to fill them automatically, this is possible for some function blocks (e.g. Mood of Lighting

Controller, Fav of Audio Player, ...). Other function blocks display the numeric value.


**Syntax:**

MENU(FunctionBlock;Input;[Value1:Text1];[Value2:Text2];...)

|Col1|Description|Comment|
|---|---|---|
|Function Block|Abbreviation of the function block to which the<br>command should be sent.||
|Input|Abbreviation of the input to which the command<br>should be sent.||
|Start value|Value that is initially set when the button is clicked and<br>starting value of the selection. If empty the current<br>value is used.<br>Special values: '+' Increases the current value, '-'<br>Decreases the current value.||
|ValueX:TextX|ValueX=Value to be sent when the text is selected.<br>TextX=Text to be shown on the display.||


## **VALUESELECT (Analog Input)____ Only relevant for the Touch Pure Flex.**


Implements a value selection via the up-down buttons on the Touch Pure Flex.

The selection range and the step size can be defined using the options of the command. If not defined

the values are taken automatically from the selected input."


**Syntax:**

VALUESELECT(FunctionBlock;Input;[Value];[MinValue];[MaxValue];[Stepsize];[Unit])


|Col1|Description|Comment|
|---|---|---|
|Function Block|Abbreviation of the function block to which the<br>command should be sent.||


Date:



2022-06-30



|Input|Abbreviation of the input to which the command<br>should be sent.|Col3|
|---|---|---|
|Start value|Value that is initially set when the button is clicked and<br>starting value of the selection. If empty the current<br>value is used.<br>Special values: '+' Increases the current value, '-'<br>Decreases the current value.||
|MinValue|Defines the allowed minimum value.||
|MaxValue|Defines the allowed maximum value.||
|Stepsize|Defines the step size.||
|Unit|Defines the unit.||

## **TIMESELECT____ Only relevant for the Touch Pure Flex.**

Implements a time selection in minutes via the up-down buttons on the Touch Pure Flex.


**Syntax:**

TIMESELECT(FunctionBlock;Input;[Start-Time];[Stepsize])


|Col1|Description|Comment|
|---|---|---|
|Function Block|Abbreviation of the function block to which the<br>command should be sent.||
|Input|Abbreviation of the input to which the command<br>should be sent.||
|Start value|Value that is initially set when the button is clicked and<br>starting value of the selection. If empty the current<br>value is used.<br>Special values: '+' Increases the current value, '-'<br>Decreases the current value.||
|Stepsize|Defines the step size.||


Date:



2022-06-30


## **WAIT____ Only relevant for the Touch Pure Flex.**

Wait for the specified duration until executing the next API command.


**Syntax:**

WAIT(Time)

|Col1|Description|Comment|
|---|---|---|
|Time|Wait time in Milliseconds||


## **GETINPUT____ Only relevant for the Touch Pure Flex.**


Get the current value from an input or a parameter of a control.


**Syntax:**

GETINPUT(FunctionBlock;Input;[Value1:Text1];[Value2:Text2];...)

|Col1|Description|Comment|
|---|---|---|
|Function Block|Abbreviation of the function block to which the<br>command should be sent.||
|Input|Abbreviation of the input from which the value should<br>be get.||
|ValueX:TextX|ValueX=Value that is replaced with the text TextX.||


## **GETOUTPUT____ Only relevant for the Touch Pure Flex.**


Get the current value from an input or a parameter of a control.


**Syntax:**

GETOUTPUT(FunctionBlock;Output;[Value1:Text1];[Value2:Text2];...)


|Col1|Description|Comment|
|---|---|---|
|Function Block|Abbreviation of the function block to which the<br>command should be sent.||
|Input|Abbreviation of the output from which the value<br>should be get.||


Date:



2022-06-30


## **ECHO____ Only relevant for the Touch Pure Flex.**

Listen to responses of function blocks and display the response. For example, a lighting controller sends

the current active light mood when it is changed. Or a shading block sends the text ‘LOCKED’ when

trying to move it while it's locked. This text is then displayed on the Touch Pure Flex and thus provides

feedback to the user.

The command is active as long as the overrun duration of the Touch Pure Flex or a different button is

pressed.


**Syntax:**

ECHO(FunctionBlock)

|Col1|Description|Comment|
|---|---|---|
|FunctionBlock|Abbreviation of the function block for which to allow to<br>display texts. All other controls are ignored.||


## **Chaining multiple commands____ Only relevant for the Touch Pure Flex.**


It is also possible to chain several commands. An = sign must be set at the beginning and the concatenation is

done with an AND sign (&). The commands are executed one after the other. When a button is pressed again, the

running command is restarted.


**Syntax:**

=Command1()&Command2()&Command3()&...

## Display Text ____ Only relevant for the Touch Pure Flex.


API commands can also be used for the display text of the Touch Pure Flex, a formula must start with a '=' here,

otherwise it will be interpreted as pure text. As an example, the volume is increased with the API command and

this is then shown on the display after a short waiting time (time is necessary for the volume to be increased,

otherwise the previous volume is displayed).

Only the WAIT, GETINPUT, GETOUTPUT & ECHO commands or texts are useful here.


Date:



2022-06-30


## **Nesting**

Nesting, for example, a GET in a SET is currently not supported!


