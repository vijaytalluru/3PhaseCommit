3PhaseCommit
============
Implementation of 3-Phase Commit protocol for a distributed song playlist application

Team members:  
Vijay Talluru   vijayt@cs.utexas.edu     EID: vt4225  
Vineet Keshari  vkeshari@cs.utexas.edu   EID: vk3226  

Slip days used (this project) : 0  
Slip days used (total)        : 0  

HOW TO RUN:

Setup:
cd src
find . -name "*.class" | xargs rm
javac ut/distcomp/threepc/Driver.java

Clear logs (start over):
rm *.log

To run N processes:
java ut/distcomp/threepc/Driver i N [-delay D] [-partialCommit p] [-deathAfter dn dp]
	N	: total no. of processes
	i	: this process' ID (0 to N-1)
	D	: delay in milliseconds (applied at all send/receive events)
	p	: if leader, commit only to process p and fail
	dn, dp	: die after receiving dn messages from process dp

Example usage (run in different tabs / terminals for clarity):
java ut/distcomp/threepc/Driver 0 5 -delay 250 -partialCommit 2 -deathAfter 1 3
java ut/distcomp/threepc/Driver 1 5 -delay 250
java ut/distcomp/threepc/Driver 2 5 -delay 250 -deathAfter 2 1
java ut/distcomp/threepc/Driver 3 5 -delay 250 -deathAfter 2 1 -deathAfter 3 4
java ut/distcomp/threepc/Driver 4 5 -delay 250

FILE STRUCTURE:
threepc/
    Driver.java             - Driver program, sets up the process and communication framework
    process/
        Site.java           - Contains code for setting up a new node, handling messages and utilities, and state variables
        Process.java        - Interface for handling send/recieve messages and input
        Leader.java         - Implements Process.java for leader
        Participant.java    - Implements Process.java for participant
        PlaylistHelper.java - Adds, removes and edits a playlist, along with logging, state update and other semantics
        StateHelper.java    - Updates state of Site
        LeaderFields.java   - Classes for storing states specific to Leader, Participant and Recovering process..
        ParticipantFields.java
        RecoveryFields.java
    util/
        Timer.java          - Create and check for timeouts
    playlist/
        Playlist.java       - Add, remove and edit a playlist

