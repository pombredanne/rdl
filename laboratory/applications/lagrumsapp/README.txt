Om exempelapplikationen
-----------------------

Syftet med denna exempelapplikation �r att illustrera i programkod hur man kan
implementera de format som rekommenderas av R�ttsinformationsprojektet f�r den
mest grundl�ggande niv�n av publicering av myndighetsf�reskrifter. Tanken �r
inte att tillhandah�lla ett f�rdigt system f�r r�ttsinformationshantering.

Applikationen har tv� delar; 1) en administrationdel i vilken man hanterar
�mnesord, f�reskrifter och grunddata och 2) en exempelwebbplats som visar
informationen f�r potentiella bes�kare. 

Applikationem �r byggd p� webbramverket Django som ges ut under BSD-licensen.
Det betyder att du f�r anv�nda och vidareutveckla koden om du vill, �ven i
kommersiella sammanhang, men att den inte kommer med n�gra garantier f�r
funktion eller l�mplighet. Se BSD-licensen f�r mer information om m�jligheter
att anv�nda dig av programkoden. 

Vi �r dock tacksamma f�r feedback och rapporter om eventuella fel. F�r mer
information om r�ttsinformationsprojektet kontakta Peter Krantz, telefon 08-561
66 921 p� Domstolsverket eller bes�k projektbloggen: 

http://rinfoprojektet.wordpress.com/



Installationsansvisningar
-------------------------

Applikationen �r baserad p� webbramverket Django
(http://www.djangoproject.com/) och �r skirven i programspr�ket Python.

Rader som b�rjar med "$" avser kommandon som skall utf�ras fr�n ett
terminalf�nster).

1. Installera programspr�ket Python 2.6 (Se http://www.python.org/download/).

2. Installera easy_install/setuptools (Se
http://pypi.python.org/pypi/setuptools och
http://peak.telecommunity.com/DevCenter/EasyInstall)

3. Installera Django
    $ easy_install django

4. Installera Sqllite 3 (Se http://www.sqlite.org/)

6. �ppna filen settings.py och modifiera vid behov de inst�llningar som b�rjar
med RINFO.

7. Installera databasschemat:
    $ python manage.py syncdb   

Efter information att tabeller skapas skall du f� en fr�ga om du vill skapa
en 'superuser'. Svara ja p� fr�gan och ange information om anv�ndaren.

8. Starta den inbyggda webbservern:
    $ python manage.py runserver

9. �ppna webbl�saren med f�ljande adress: http://127.0.0.1:8000/
Exempelwebbplatsen visas. F�r att redigera inneh�ll navigera till
http://127.0.0.1:8000/admin/ och logga in som den anv�ndare du skapade i steg 7.

10. Verifiera att applikationen �r installerad korrekt genom att k�ra de
automatiska testerna med:
    $ python manage.py test

F�r du problem med isntallationen se f�ljande k�llor:

http://docs.djangoproject.com/en/dev/intro/install/


N�sta steg
----------

Applikationen illustrerar hur f�reskrifter relateras till varandra och hur
metadata av olika slag kan f�ngas upp och presenteras p� ett standardiserat
s�tt. 

B�rja med att l�gga in i administrationsgr�nssnittet och skapa lite grunddata
(F�rfattningssamling, n�gra �mnesord och bemyndigandeparagrafer).

N�gra saker att utg� fr�n:

1. Filen urls.py visar vilka olika typer av l�nkar som webbplatsen hanterar.
Varje l�nkformat �r kopplat till en metod i rinfo/views.py. 

2. I rinfo/models.py hittar du klasserna f�r de olika informationsobjekten.
Klassen Myndighetsforeskrift visar n�gra olika typer av metadata och relationer
till andra objekt. 

3. Mallen templates/foreskrift_rdf.xml visar hur en grundl�ggande metadatapost
�r uppbyggd.

4. Atomfeeden ber�ttar om f�r�ndringar som skett med poster i samlingen. Feeden
finns p� adressen http://127.0.0.1:8000/feed/. Nya poster, uppdateringar av
poster (skall inte ske om man inte gjort fel tidigare) och radering av poster
(i h�ndelse av en grov felpublicering) g�r att ett AtomEntry-objekt skapas.
Dessa sammanst�lls i en feed i templates/atomfeed.xml.

Eftersom applikationen �r baserad p� ramverket Django kan det vara bra att
k�nna till grunderna om detta. Mer information om Django hittar du h�r:
http://docs.djangoproject.com/en/dev/intro/overview/
