# ExpenseOptimizer

## Opis Zadania
Celem projektu było opracowanie algorytmu, który dla zadanej listy 
zamówień, dostępnych promocji oraz portfela klienta (metody płatności, 
limity, rabaty) dobierze optymalny sposób płatności dla każdego 
zamówienia. Algorytm dąży do maksymalizacji łącznego rabatu przy 
jednoczesnym spełnieniu wszystkich ograniczeń (pełne opłacenie zamówień, 
nieprzekraczanie limitów metod płatności). Dodatkowo algorytm 
minimalizuje płatności kartami, preferując wydawanie punktów 
lojalnościowych, jeśli nie zmniejsza to należnego rabatu.

## Zasady Płatności i Rabatów

### Opcje Płatności za Zamówienie:
Każde zamówienie można opłacić:
1.  W całości za pomocą jednej tradycyjnej metody płatności (np. jedną kartą).
2.  W całości punktami lojalnościowymi.
3.  Częściowo punktami i częściowo jedną tradycyjną metodą płatności.

### Rabaty:
1.  **Rabaty Bankowe:**
    *   Oferowane na podstawie umów z wybranymi bankami.
    *   Aplikowane **tylko wtedy**, gdy zamówienie zostanie opłacone **w całości**
        kartą konkretnego banku objętą promocją.
    *   Każde zamówienie ma przypisany podzbiór promocji (metod płatności), które 
        mogą być na nim zastosowane.
    *   System nie wspiera rabatów bankowych przy częściowych płatnościach kartą.
2.  **Rabaty za Punkty Lojalnościowe:**
    *   **Płatność w całości punktami ("PUNKTY"):** Jeśli całe zamówienie jest 
        opłacane punktami, stosowany jest rabat procentowy zdefiniowany dla 
        metody "PUNKTY".
    *   **Częściowa płatność punktami ("PUNKTY\_10"):** Jeśli klient opłaci **co 
        najmniej 10% wartości zamówienia** (przed rabatem) punktami lojalnościowymi, 
        sklep nalicza dodatkowy rabat w wysokości **10% na całe zamówienie**.
        *   Zastosowanie tego rabatu wyklucza możliwość aplikacji dodatkowego rabatu 
            przez użycie karty bankowej dla pozostałej części płatności.
    *   Promocje za użycie punktów (PUNKTY i PUNKTY\_10) nie jest przypisana do 
        zamówień i można ją zastosować zawsze (o ile spełnione są warunki).

## Struktura Danych Wejściowych

Aplikacja przyjmuje dwa pliki JSON jako argumenty wiersza poleceń:

1.  **`orders.json`**: Lista zamówień (do 10000):
    ```
    [
        {"id": "ORDER1", "value": "100.00", "promotions": ["mZysk"]},
        {"id": "ORDER2", "value": "200.00", "promotions": ["BosBankrut"]},
        {"id": "ORDER3", "value": "150.00", "promotions": ["mZysk", "BosBankrut"]},
        {"id": "ORDER4", "value": "50.00"}
    ]
    ```
    Gdzie:
    *   `id`: Identyfikator zamówienia.
    *   `value`: kwota zamówienia z dokładnością do dwóch miejsc po przecinku.
    *   `promotions`: lista identyfikatorów promocji zależnych od metod płatności, które
        mogą być zaaplikowane do tego zamówienia. Nazwy tych promocji są tożsame z
        nazwami metod płatności.
        * Lista ta jest opcjonalna, a gdy nie jest zdefiniowana, można stosować wyłącznie promocje za wydane punkty.
        * Nawet kiedy karta z dostępnych metod płatności nie jest zdefiniowana w liście “promotions”, wciąż można jej 
          użyć do opłacenia zamówienia. W takiej sytuacji nie można jedynie zastosować związanej z nią promocją.

2.  **`paymentmethods.json`**: Lista posiadanych przez klienta metod płatności (do 1000):
    ```
    [
        {"id": "PUNKTY", "discount": "15", "limit": "100.00"},
        {"id": "mZysk", "discount": "10", "limit": "180.00"},
        {"id": "BosBankrut", "discount": "5", "limit": "200.00"}
    ]
    ```
    Gdzie:
    *   `id`: Nazwa metody płatności / promocji. Dla punktów lojalnościowych zawsze "PUNKTY".
    *   `discount`: Liczba całkowita określająca procentowy rabat.
    *   `limit`: Maksymalna kwota dostępna w danej metodzie płatności (z dokładnością do dwóch miejsc po przecinku).

## Opis Działania Algorytmu

Algorytm jest wariacją podejścia zachłannego, iterującą po zamówieniach i starającą się wybrać dla każdego z nich najlepszą możliwą 
opcję płatności pod kątem maksymalizacji rabatu, z uwzględnieniem zdefiniowanych heurystyk oraz predefiniowanych ograniczeń i priorytetów.

### Faza 1: Główna Optymalizacja Płatności

1.  **Sortowanie Zamówień:** 
    Zamówienia są sortowane malejąco według ich wartości (`value`).
2.  **Iteracja po Zamówieniach:** Dla każdego zamówienia podejmowane są następujące kroki:
    *  **Analiza Dostępnych Promocji:** Identyfikowane są wszystkie możliwe strategie płatności (pełna płatność kartą promocyjną, pełna płatność punktami "PUNKTY", częściowa płatność punktami "PUNKTY\_10") wraz z odpowiadającymi im rabatami procentowymi.
    *  **Wybór Najlepszej Strategii:** Wybierana jest strategia dająca najwyższy procentowy upust.
        *   **Brak remisu:** Jeśli jedna strategia jest wyraźnie najlepsza, zamówienie jest opłacane tą metodą.
        *   **Obsługa Remisów:**
            *   **Remis PEŁNE\_PUNKTY vs KARTA:** Zawsze wybierana jest opcja płatności punktami (zgodnie z priorytetem minimalizacji użycia kart, jeśli rabat jest ten sam).
            *   **Remis KARTA vs KARTA (dwie różne karty dają identyczny rabat):**
                1.  Wybierana jest karta, która jest przypisana jako promocja do mniejszej liczby pozostałych, nieopłaconych jeszcze zamówień (oszczędzanie "częstszych" promocji).
                2.  W przypadku dalszego remisu, wybierana jest karta z mniejszym aktualnym limitem środków (oszczędzanie kart o większej pojemności).
            *   **Remis PUNKTY\_10 vs KARTA (strategia częściowych punktów daje taki sam rabat jak pełna płatność kartą):**
                1.  Zawsze wybierana jest strategia PUNKTY\_10 (priorytet dla punktów).
                2.  **Wybór karty do dopłaty reszty:**
                    *   **Jeśli istnieją karty z wystarczającym limitem płatności:** 
                        *   Preferowana jest karta, która *nie jest* promocją dla żadnego innego nieopłaconego zamówienia ("bezpieczna karta"). Jeśli takich kart jest wiele, wybierana jest ta z mniejszym pozostałym limitem.
                        *   Jeśli nie ma "bezpiecznych" kart, wybierana jest karta (spośród wszystkich zdefiniowanych), która ma najmniejszy rabat.
                        *   W przypadku dalszych remisów przy wyborze karty do dopłaty stosowane są kryteria jak w remisie KARTA-KARTA (rzadkość w przyszłych promocjach, mniejszy limit).
                    *   **Jeśli brak kart z wystarczającym limitem na standardową dopłatę PUNKTY\_10:** 
                        * Algorytm próbuje znaleźć kartę z maksymalnym dostępnym limitem (który jest mniejszy niż wymagana dopłata) i jeśli jest ich wiele, wybiera najlepszą z nich według kryteriów zdefiniowanych powyżej w sekcji **Jeśli istnieją karty z wystarczającym limitem płatności**. 
                        * Resztę (różnicę między wartością zamówienia po rabacie 10% a limitem wybranej karty) próbuje pokryć punktami.
                        * W przypadku, gdy algorytm dojdzie do tego miejsca i pozostałych punktów nie wystarczy na dopłacenie pozostałej kwoty zamówienia, algorytm kończy działanie bez znalezienia rozwiązania -> doszedł do momentu, gdzie nie jest w stanie dobrać żadnej metody płatności do aktualnie przetwarzanego zamówienia 

    c.  **Aktualizacja Stanu:** Po dokonaniu wyboru i opłaceniu zamówienia, aktualizowane są limity użytych metod płatności oraz wewnętrzny licznik `ordersAmount` dla kart promocyjnych (wskazujący, dla ilu jeszcze zamówień dana karta może stanowić promocję).

### Faza 2: Wydawanie Pozostałych Punktów (Minimalizacja Płatności Kartami)

Po przetworzeniu wszystkich zamówień w Fazie 1, jeśli klientowi pozostały niewykorzystane punkty lojalnościowe, algorytm próbuje je wydać:

1.  **Warunek:** Ta faza jest uruchamiana tylko, jeśli wszystkie zamówienia zostały opłacone w Fazie 1 i pozostały punkty.
2.  **Iteracja Wsteczna:** Algorytm iteruje po zamówieniach w odwrotnej kolejności ich przetwarzania (od ostatnio opłaconego do pierwszego).
3.  **Identyfikacja Kandydatów:** Szukane są zamówienia, które zostały opłacone z wykorzystaniem strategii PUNKTY\_10 (czyli częściowo punktami, częściowo kartą).
4.  **Zastępowanie Płatności Karty płatnością Punktami:** Dla znalezionego zamówienia:
    *   Określana jest kwota zapłacona kartą w ramach strategii PUNKTY\_10.
    *   Algorytm "zwraca" na użytą kartę maksymalną możliwą kwotę (nie więcej niż pierwotnie nią zapłacono i nie więcej niż aktualnie dostępnych punktów).
    *   Ta zwrócona kwota jest następnie "opłacana" z pozostałych punktów lojalnościowych.
5.  **Zakończenie Fazy:** Proces jest kontynuowany, aż do wyczerpania wszystkich punktów lojalnościowych lub przejrzenia wszystkich zamówień.

### Wynik Działania Algorytmu

Metoda `optimize()` zwraca:
*   `true`: Jeśli udało się opłacić wszystkie zamówienia.
*   `false`: Jeśli w trakcie przetwarzania algorytm napotkał zamówienie, którego nie można było opłacić żadną dostępną kombinacją metod płatności przy zachowaniu limitów.

W przypadku, gdy metoda `optimize()` zwróci `true`, na standardowe wyjście zostaną wypisane statystyki dla każdej metody płatności w formie `<id_metody> <wydana_kwota>`, np.:
```
PUNKTY 150.0
SuperKarta 150.0
ZwyklaKarta 0.0
MegaBank 170.0
```

## Uruchomienie Aplikacji

Aplikacja jest uruchamiana z linii poleceń za pomocą Javy 21:

```bash
java -jar /sciezka/do/aplikacji/ExpenseOptimizer.jar /sciezka/do/orders.json /sciezka/do/paymentmethods.json