# Debyter, the bytecode debugger
## Usage
### Attach to JVM
The JVM with debuggee application should be started. 
It can be both suspended or not, the classes don't have to be loaded right away. 
Examples of starting the JVM (the specified compiled class is present as example in this project):

`java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=127.0.0.1:8000 smthelusive/debyter/examples/TheDebuggee`

`java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:8000 smthelusive/debyter/examples/TheDebuggee`

The Debyter accepts address as input parameter in the following format:
`{host}:{port}`, example: `127.0.0.1:8000`.\
The above is also a default address in case no other address is provided.

### Set the breakpoint
In order to set the breakpoint, use the command `bp` or `breakpoint`. Examples:

`bp smthelusive.debyter.examples.TheDebuggee main 15` 
`breakpoint smthelusive.debyter.examples.TheDebuggee main 15` 

where: 

- `smthelusive.debyter.examples.TheDebuggee` is the fully qualified class name;
- `main` is the method name;
- `15` is the code index.

To know which code index to use, use `javap` tool 
that shows the bytecode in human-readable format. For example above, the
following can be executed (from inside `src` directory):

`javap -c smthelusive/debyter/examples/TheDebuggee`

Result will look like this:

```text
Compiled from "TheDebuggee.java"
public class smthelusive.debyter.examples.TheDebuggee {
  public smthelusive.debyter.examples.TheDebuggee();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public static void main(java.lang.String[]);
    Code:
       0: iconst_0
       1: istore_1
       2: iinc          1, 88
       5: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
       8: iload_1
       9: invokevirtual #13                 // Method java/io/PrintStream.print:(I)V
      12: iinc          1, -1
      15: iload_1
      16: iload_1
      17: imul
      18: istore_1
      19: iload_1
      20: invokedynamic #19,  0             // InvokeDynamic #0:makeConcatWithConstants:(I)Ljava/lang/String;
      25: astore_2
      26: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
      29: aload_2
      30: invokedynamic #23,  0             // InvokeDynamic #1:makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;
      35: invokevirtual #26                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      38: return
}
```

Which means that if we place a breakpoint in code index `15` of method `main`,
we will stop at bytecode operation `iload_1`, 
right after the integer value has been decremented, 
so at this point integer value should be equal to `87`.

All local variables are logged immediately when the breakpoint is hit.

### Remove the breakpoint
To remove the breakpoint, use the command `rm` or `remove`. Examples:

`rm smthelusive.debyter.examples.TheDebuggee main 15`
`remove smthelusive.debyter.examples.TheDebuggee main 15`

where:

- `smthelusive.debyter.examples.TheDebuggee` is the fully qualified class name;
- `main` is the method name;
- `15` is the code index.

### Suspend
To suspend target JVM, use command `suspend` or `pause`.

### Resume the JVM
The JVM can be in the suspended state for the following reasons:
- it was started in suspended mode
- suspend command was executed before
- the breakpoint is hit
- the step over is hit

Use `resume` or `run` command to continue execution.

Important remark from the oracle docs:

`Suspensions of the Virtual Machine and individual threads are counted. 
If a particular thread is suspended n times, it must resumed n times before it will continue.`

### Step over
Use `step` command to step over to the next line (bytecode operation).

All local variables are logged immediately when the step over is hit.

### Clear breakpoints
Use `clear` command to remove all the breakpoints.

### Stop the debuggee app execution
It is possible to terminate execution of the debuggee app. Use command `stop` or `terminate`.

### Exit
Use `exit` command to exit the debugger. The debuggee application will not be affected.

### Other remarks
All local variables are logged, but not all of their values are parsed. 
Debugger currently only supports primitive types, Strings and arrays (containing primitive types and Strings).
For any other objects only references are logged.

## Implementation details
Debugger connects to remotely running JVM with remote debugging enabled.
The communication between debugger and JVM happens through the [JDWP](https://docs.oracle.com/javase/7/docs/technotes/guides/jpda/jdwp-spec.html) protocol.
Communication through JDWP is asynchronous.

`ResponseProcessor` is running in a separate thread, it listens to any responses from JVM, parses them and notifies listeners.\
`UserInputProcessor` is running in a separate thread, it reads user commands from console, validates them and notifies listeners.\
`Debyter` is running in the main thread. It listens to `ResponseProcessor` and `UserInputProcessor`,
and puts the user commands and JVM responses onto dedicated queues. 
It continuously in a loop consumes from the queues: 
- a single user command,
- all available JVM responses.

`Debyter` is also responsible for sending requests to JVM. 

Current design isn't great, the flow of requesting information from JVM 
and collecting enough information for performing different request has grown overcomplicated.

## Contributing
This project was intended as a PoC, but if it's useful as a tool and receives some love, that's great! 
Contributions are very welcome.
For any questions & suggestions please reach out: smthelusive@gmail.com