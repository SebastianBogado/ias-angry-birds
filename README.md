# First steps

Follow the steps here: [english](https://github.com/SebastianBogado/cim2-angry-birds/blob/master/doc/ABDoc.pdf),
 [spanish](https://github.com/SebastianBogado/cim2-angry-birds/blob/master/doc/tp_angryBirds_1raEntrega.pdf)

# Discontinued Angry Birds

As the Chrome version has been disctoninued, there's a work around in which
you modifiy Chrome's cache so that Angry Birds' web is loaded from there, as
explained in the previous docs. [Modified cache](https://drive.google.com/file/d/0B3T3L0K6Tm8Rb1U1NktKSnF1bDg/view)

Provided is a an example of a script that automates this work around: 
`chromeAngryBirds.sh.example`. Just remove the '.example' extension and adapt 
it to your environment. Mind that **whenever you run this script, it terminates
every running Chrome process**



# Build the project
`ant compile jar`

# Run the project
`java -jar ABSoftware.jar`
