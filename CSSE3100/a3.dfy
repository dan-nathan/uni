// Question 1
function method abs(x: int): int { if x >= 0 then x else -1 * x }

method largestDifference(a: array<int>) returns (x: int)
    requires a.Length >= 2
    ensures forall i :: 0 <= i < a.Length - 1 ==> abs(a[i + 1] - a[i]) <= x
    ensures exists i :: 0 <= i < a.Length - 1 && abs(a[i + 1] - a[i]) == x
{
    x := abs(a[1] - a[0]);
    var n := 2;
    while n != a.Length
        invariant 0 <= n <= a.Length
        invariant forall i :: 0 <= i < n - 1 ==> abs(a[i + 1] - a[i]) <= x
        invariant exists i :: 0 <= i < n - 1 && abs(a[i + 1] - a[i]) == x
        decreases a.Length - n
    {
        if abs(a[n] - a[n - 1]) > x {
            x := abs(a[n] - a[n - 1]);
        }
        n := n + 1;
    }
}

// Question 2
method reflect<T>(a: array<T>)
    modifies a
    ensures forall i :: 0 <= i < a.Length / 2 ==> a[i] == old(a[i])
    ensures forall i :: 0 <= i < a.Length / 2 ==> a[i] == a[a.Length - 1 - i]
{
    var n := 0;
    while n != a.Length / 2
        invariant 0 <= n <= a.Length / 2
        invariant forall i :: 0 <= i < a.Length / 2 ==> a[i] == old(a[i])
        invariant forall i :: 0 <= i < n ==> a[i] == a[a.Length - 1 - i]
        decreases a.Length / 2 - n
    {
        a[a.Length - 1 - n] := a[n];
        n := n + 1;
    }    
}